//
// Created by 葛祥林 on 12/22/20.
//

/**
 * fake_dlopen()与fake_dlsym()可以代替dlopen()与dlsym()来使用，它的原理是在当前进程的内存中，
 * 搜索符号表的方式，在内存中找到函数的内存地址。当然，它是有限制的：只能dlopen()已经加载进入内存
 * 的so，即系统或自己预先加载的动态库，并且参数flags加载标志被忽略。
 */

#include "elfop.h"

#ifdef __cplusplus
extern "C" {
#endif

static int _dlclose(void* handle) {
  if (handle) {
    auto* elf_ctx = reinterpret_cast<elf_ctx_t*>(handle);
    if (elf_ctx->dynsym) {
      free(elf_ctx->dynsym);    /* we're saving dynsym and dynstr */
    }
    if (elf_ctx->dynstr) {
      free(elf_ctx->dynstr);    /* from library file just in case */
    }
    free(elf_ctx);
  }
  return 0;
}

/** 
 * 缺点: flags are ignored
 * 
 * API>=24 的 Android 系统，Google 限制不能使用dl库，那么可以通过 /proc/pid/maps 文件，
 * 来查找到对应so库文件，被加载到该进程中的基地址，从而把该文件使用 mmap() 映射到内存中，这里
 * 是为了获取 elf 文件中每个符号的相对偏移量，再加上 基地址，就是在进程地址空间中真正的地址;
 * 再进行解析，获取 so库(ELF) 中的符号表类型的节、字符串类型的节，打包成 struct ctx 返回
 */
static void* getArtCtx(const char* libpath, int flags) {
  FILE* maps;
  char buff[256];
  elf_ctx_t* ctx = nullptr;
  off_t load_addr;
  off_t size;
  int i;
  int fd = -1;
  bool found = false;
  char* shoff;

  // ELF文件头，这里是把so库文件使用 mmap() 系统调用，映射到这个地址
  auto* elf = (Elf_Ehdr*) MAP_FAILED; // reinterpret_cast<void*>(-1)

#define fatal(fmt, args...) do {    \
            log_err(fmt,##args);    \
            goto err_exit;          \
        } while(0)

  maps = fopen("/proc/self/maps", "r");
  if (maps == nullptr) {
    fatal("failed to open maps");
  }

  // 查找到 libpath 这个so库的行
  while (!found && fgets(buff, sizeof(buff), maps)) {
    if (strstr(buff, libpath) && (strstr(buff, "r-xp") || strstr(buff, "r--p"))) {
      found = true;
    }
  }
  fclose(maps);

  // FIXME: 前面是为了获取 libart.so 加载到进程中地址空间的内存起始地址和结束地址

  if (!found) {
    fatal("%s not found in my userspace", libpath);
  }

  if (std::sscanf(buff, "%lx", &load_addr) != 1) {
    fatal("failed to read load address for %s", libpath);
  }

  log_info("%s loaded in Android at 0x%08lx", libpath, load_addr);

  // Now, mmap the same library once again

  fd = open(libpath, O_RDONLY);
  if (fd < 0) {
    fatal("failed to open %s", libpath);
  }
  size = lseek(fd, 0, SEEK_END); // 索引到so库文件末尾
  if (size <= 0) {
    fatal("lseek() failed for %s", libpath);
  }

  // mmap()是Linux/Unix的系统调用，映射文件或设备到内存中，取消映射就是munmap()函数.
  // - 该函数主要用途有三个：
  // 1. 将普通文件映射到内存中，通常在需要对文件进行频繁读写时使用，用内存读写取代I/O读写，
  //    以获得较高的性能；
  // 2. 将特殊文件进行匿名内存映射，为关联进程提供共享内存空间；
  // 3. 为无关联的进程间的Posix共享内存（SystemV的共享内存操作是shmget/shmat）
  // - 参数addr：
  // 指向欲映射的内存起始地址，通常设为 nullptr，代表让系统自动选定地址，映射成功后返回该地址。
  // - 参数length：
  // 代表将文件中多大的部分映射到内存。
  // - 参数prot：
  // 映射区域的保护方式。可以为以下几种方式的组合：
  // PROT_EXEC 映射区域可被执行
  // PROT_READ 映射区域可被读取
  // PROT_WRITE 映射区域可被写入
  // PROT_NONE 映射区域不能存取
  // - 参数flags：
  // 影响映射区域的各种特性。在调用mmap()时必须要指定 MAP_SHARED 或 MAP_PRIVATE。
  // MAP_FIXED 如果参数start所指的地址无法成功建立映射时，则放弃映射，不对地址做修正。
  //           通常不鼓励用此.
  // MAP_SHARED对映射区域的写入数据会复制回文件内，而且允许其他映射该文件的进程共享。
  // MAP_PRIVATE 对映射区域的写入操作会产生一个映射文件的复制，即私人的“写入时复制”
  //            （copy on write）对此区域作的任何修改都不会写回原来的文件内容。
  // MAP_ANONYMOUS 建立匿名映射。此时会忽略参数fd，不涉及文件，而且映射区域无法和其他进程共享。
  // MAP_DENYWRITE 只允许对映射区域的写入操作，其他对文件直接写入的操作将会被拒绝。
  // MAP_LOCKED 将映射区域锁定住，这表示该区域不会被置换（swap）。
  // - 参数fd：
  // 要映射到内存中的文件描述符。如果使用匿名内存映射时，即flags中设置了MAP_ANONYMOUS，fd设为-1
  // - 参数offset：
  // 文件映射的偏移量，通常设置为0，代表从文件最前方开始对应，offset必须是分页大小的整数倍
  elf = (Elf_Ehdr*) mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0);
  close(fd);
  fd = -1; // 安全防御性编程，防止fd被滥用，导致的一些未定义行为
  if (elf == MAP_FAILED) {
    fatal("mmap() failed for %s", libpath);
  }
  // FIXME：这里使用 mmap 的目的是为了获取到 libart.so 这个 elf 文件内各个符号的偏移量(基地址不同，但偏移量相同)

  ctx = reinterpret_cast<elf_ctx_t*>(calloc(1, sizeof(elf_ctx_t)));
  if (!ctx) {
    fatal("no memory for %s", libpath);
  }

  ctx->load_addr = (void*) load_addr;   // so库加载到该进程中的基地址
  shoff = ((char*) elf) + elf->e_shoff; // 节头表偏移量

  // 遍历节头表(Section Header Table)
  for (i = 0; i < elf->e_shnum; i++, shoff += elf->e_shentsize) {
    auto* sh = (Elf_Shdr*) shoff; // 节头
    log_dbg("%s: i=%d shdr=%p type=%x", __func__, i, sh, sh->sh_type);

    switch (sh->sh_type) {
    case SHT_DYNSYM: { // 符号表 .dynsym
      if (ctx->dynsym) {
        fatal("%s: duplicate DYNSYM sections", libpath); /* .dynsym */
      }
      ctx->dynsym = malloc(sh->sh_size);
      if (!ctx->dynsym) {
        fatal("%s: no memory for .dynsym", libpath);
      }
      memcpy(ctx->dynsym, ((char*) elf) + sh->sh_offset, sh->sh_size);
      ctx->nsyms = sh->sh_size / sizeof(Elf_Sym);
    }
    break;

    case SHT_STRTAB: { // 字符串(名字)表 .dynstr
      if (ctx->dynstr) {
        break;    /* .dynstr is guaranteed to be the first STRTAB */
      }
      ctx->dynstr = malloc(sh->sh_size);
      if (!ctx->dynstr) {
        fatal("%s: no memory for .dynstr", libpath);
      }
      memcpy(ctx->dynstr, ((char*) elf) + sh->sh_offset, sh->sh_size);
    }
    break;

    case SHT_PROGBITS: {
      if (!ctx->dynstr || !ctx->dynsym) {
        break;
      }
      // won't even bother checking against the section name
      // - sh_addr: 该节在 elf 文件被加载到进程地址空间中后的偏移量，其在进程中的真实地址是:
      //            load_addr + sh->sh_addr.
      // - sh_offset 在 elf 文件中的偏移量
      ctx->bias = (off_t) sh->sh_addr - (off_t) sh->sh_offset; // TODO: ing......
      i = elf->e_shnum;  /* exit for */
    }
    break;
    }
  }

  munmap(elf, size); // 释放elf在mmap空间的映射
  elf = nullptr;

  if (!ctx->dynstr || !ctx->dynsym) {
    fatal("dynamic sections not found in %s", libpath);
  }

#undef fatal
  log_dbg("%s: ok, dynsym = %p, dynstr = %p", libpath, ctx->dynsym, ctx->dynstr);
  return ctx;

err_exit:
  if (fd >= 0) {
    close(fd);
  }
  if (elf != MAP_FAILED) {
    munmap(elf, size);
  }
  dlclose_elf(ctx);
  return nullptr;
}

/**
 * 由于google限制了 api>=24 以上的Android系统使用dlfcn.h库，所以这里做特殊处理
 */
static void* _dlopen(const char* filename, int flags) {
  if (strlen(filename) > 0 && filename[0] == '/') { // so库的完整路径
    return getArtCtx(filename, flags);
  } else { // so库的非完整路径
    char buf[512] = {0};

    // sysmtem
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kApexLibDir_11);
    strcat(buf, filename); // 生成so库完整路径
    logi("kApexLibDir_11=%s", buf);
    void* context = getArtCtx(buf, flags);
    if (context) {
      return context;
    }

    // sysmtem
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kSystemLibDir);
    strcat(buf, filename); // 生成so库完整路径
    logi("kSystemLibDir=%s", buf);
    context = getArtCtx(buf, flags);
    if (context) {
      return context;
    }

    // apex
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kApexLibDir);
    strcat(buf, filename);
    logi("kApexLibDir=%s", buf);
    context = getArtCtx(buf, flags);
    if (context) {
      return context;
    }

    // odm
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kOdmLibDir);
    strcat(buf, filename);
    context = getArtCtx(buf, flags);
    if (context) {
      return context;
    }

    // vendor
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kVendorLibDir);
    strcat(buf, filename);
    context = getArtCtx(buf, flags);
    if (context) {
      return context;
    }

    return getArtCtx(filename, flags);
  }
}

//
//
//if ( idx != string::npos )

/**
 * 从 符号表(节) 中获取 name 指定的符号，这里需要注意的是：
 * 1. 符号表中的 sym->st_name 字段不仅仅是一个字符串名称，还是一个index，该位置对应的就是符号名称.
 * 2. sym->st_value 字段表示的是该字符对应的地址偏移.
 */
static void* _dlsym(void* context, const char* symbol_name) {
  auto* ctx = reinterpret_cast<elf_ctx_t*>(context);
  auto* sym = reinterpret_cast<Elf_Sym*>(ctx->dynsym);
  char* strings = reinterpret_cast<char*>(ctx->dynstr);

  for (int i = 0; i < ctx->nsyms; ++i, ++sym) { // 遍历符号表
    if (strcmp(strings + sym->st_name, symbol_name) == 0) { // 找到该符号(函数符号)
      // NB: sym->st_value is an offset into the section for relocatables,
      // but a VMA for shared libs or exe files, so we have to subtract
      // the bias.
      // 在so库或可执行文件中，sym->st_value 表示符号的地址
      // ctx->bias = (off_t) sh->sh_addr - (off_t) sh->sh_offset
      void* ret = (char*) ctx->load_addr + sym->st_value - ctx->bias;
      log_info("%s found at %p", symbol_name, ret);
      return ret;
    }
  }
  return nullptr;
}

static const char* _dlerror() {
  return nullptr;
}

// =============== implementation for compat ==========
static int SDK_INT = -1;

static int get_sdk_level() {
  if (SDK_INT > 0) {
    return SDK_INT;
  }
  char sdk[PROP_VALUE_MAX] = {0};
  __system_property_get("ro.build.version.sdk", sdk);
  SDK_INT = std::atoi(sdk);
  logd("get_sdk_level, SDK_INT=%d", SDK_INT);
  return SDK_INT;
}

/**
 * 返回的是 struct ctx 结构体
 */
__unused
void* dlopen_elf(const char* filename, int flags) {
  log_info("dlopen: %s", filename);

  if (get_sdk_level() >= 24) {
    return _dlopen(filename, flags);
  } else {
    return dlopen(filename, flags);
  }
}

__unused
void* dlsym_elf(void* handle, const char* symbol) {
  if (get_sdk_level() >= 24) {
    return _dlsym(handle, symbol);
  } else {
    return dlsym(handle, symbol);
  }
}

__unused
int dlclose_elf(void* handle) {
  if (get_sdk_level() >= 24) {
    return _dlclose(handle);
  } else {
    return dlclose(handle);
  }
}

__unused
const char* dlerror_elf() {
  if (get_sdk_level() >= 24) {
    return _dlerror();
  } else {
    return dlerror();
  }
}

#ifdef __cplusplus
} // end of "extern "C""
#endif
