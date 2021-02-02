//
// Created by 葛祥林 on 1/21/21.
//

#include "elf_op.h"
#include "constants.h"
#include <functional>

namespace iwatch {

void* Elf::dlopen_elf(const char* filename, int flags) {
  log_info("dlopen: %s", filename);

  if (get_sdk_level() >= 24) {
    return _dlopen(filename, flags);
  } else {
    this->load_addr_ptr = dlopen(filename, flags);
    return this->load_addr_ptr;
  }
}

void* Elf::dlsym_elf(const char* symbol_name) {
  if (get_sdk_level() >= 24) {
    return _dlsym(symbol_name);
  } else {
    return dlsym(this->load_addr_ptr, symbol_name);
  }
}

int Elf::dlclose_elf() {
  if (get_sdk_level() >= 24) {
    return _dlclose();
  } else {
    return dlclose(this->load_addr_ptr);
  }
}

const char* Elf::dlerror_elf() {
  if (get_sdk_level() >= 24) {
    return _dlerror();
  } else {
    return dlerror();
  }
}

void* Elf::_dlopen(const char* filename, int flags) {
  if (strlen(filename) > 0 && filename[0] == '/') { // so库的完整路径
    return initElf(filename);
  } else { // so库的非完整路径
    char buf[512] = {0};

    // Android-11: apex/com.android.art/lib(64)
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kApexLibDir_11);
    strcat(buf, filename); // 生成so库完整路径
    logi("kApexLibDir_11=%s", buf);
    void* load_addr = initElf(buf);
    if (load_addr != nullptr) {
      return load_addr;
    }

    // sysmtem
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kSystemLibDir);
    strcat(buf, filename); // 生成so库完整路径
    logi("kSystemLibDir=%s", buf);
    load_addr = initElf(buf);
    if (load_addr != nullptr) {
      return load_addr;
    }

    // apex
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kApexLibDir);
    strcat(buf, filename);
    logi("kApexLibDir=%s", buf);
    load_addr = initElf(buf);
    if (load_addr != nullptr) {
      return load_addr;
    }

    // odm
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kOdmLibDir);
    strcat(buf, filename);
    load_addr = initElf(buf);
    if (load_addr != nullptr) {
      return load_addr;
    }

    // vendor
    memset(buf, 0, sizeof(buf));
    strcpy(buf, kVendorLibDir);
    strcat(buf, filename);
    load_addr = initElf(buf);
    if (load_addr != nullptr) {
      return load_addr;
    }

    return initElf(filename);
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
void* Elf::_dlsym(const char* symbol_name) {
  auto sym = reinterpret_cast<Elf_Sym*>(this->dynsym);
  char* strings = reinterpret_cast<char*>(this->dynstr);

  for (int i = 0; i < this->nsyms; ++i, ++sym) { // 遍历符号表
    if (strcmp(strings + sym->st_name, symbol_name) == 0) { // 找到该符号(函数符号)
      // NB: sym->st_value is an offset into the section for relocatables,
      // but a VMA for shared libs or exe files, so we have to subtract
      // the bias.
      // 在so库或可执行文件中，sym->st_value 表示符号的地址
      // ctx->bias = (off_t) sh->sh_addr - (off_t) sh->sh_offset
      void* symbol_addr = reinterpret_cast<int8_t*>(this->load_addr_ptr) + sym->st_value - this->bias;
      log_info("%s found at %p", symbol_name, symbol_addr);
      return symbol_addr;
    }
  }
  return nullptr;
}

const char* Elf::_dlerror() {
  return nullptr;
}

// =============== implementation for compat ==========

int Elf::get_sdk_level() {
  if (SDK_INT > 0) {
    return SDK_INT;
  }
  char sdk[PROP_VALUE_MAX] = {0};
  __system_property_get("ro.build.version.sdk", sdk);
  SDK_INT = std::atoi(sdk);
  logd("get_sdk_level, SDK_INT=%d", SDK_INT);
  return SDK_INT;
}

int Elf::_dlclose() {
  if (this->dynsym != nullptr) {
    free(this->dynsym);    /* we're saving dynsym and dynstr */
    this->dynsym = nullptr;
  }
  if (this->dynstr != nullptr) {
    free(this->dynstr);    /* from library file just in case */
    this->dynstr = nullptr;
  }
  this->load_addr_ptr = nullptr;
  return I_OK;
}

void* Elf::initElf(const char* libpath) {
  FILE* maps;
  char buff[256];
  off_t load_addr;
  off_t size;
  int i;
  int fd = -1;
  bool found = false;
  int8_t* shoff;

  // ELF文件头，这里是把so库文件使用 mmap() 系统调用，映射到这个地址
  auto elf = reinterpret_cast<Elf_Ehdr*>(MAP_FAILED); // reinterpret_cast<void*>(-1)

  std::function<void* ()> exception_quit = [this, &fd, &elf, &size]() -> void* {
    if (fd >= 0) {
      close(fd);
    }
    if (elf != MAP_FAILED) {
      munmap(elf, size);
    }

    this->dlclose_elf();

    return nullptr;
  };

  maps = fopen("/proc/self/maps", "r");
  if (maps == nullptr) {
    log_err("failed to open maps");
    return exception_quit();
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
    log_err("%s not found in my userspace", libpath);
    return exception_quit();
  }

  if (std::sscanf(buff, "%lx", &load_addr) != 1) {
    log_err("failed to read load address for %s", libpath);
    return exception_quit();
  }

  log_info("%s loaded in Android at 0x%08lx", libpath, load_addr);

  // Now, mmap the same library once again

  fd = open(libpath, O_RDONLY);
  if (fd < 0) {
    log_err("failed to open %s", libpath);
    return exception_quit();
  }
  size = lseek(fd, 0, SEEK_END); // 索引到so库文件末尾
  if (size <= 0) {
    log_err("lseek() failed for %s", libpath);
    return exception_quit();
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
  elf = reinterpret_cast<Elf_Ehdr*>(mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0));
  close(fd);
  fd = -1; // 安全防御性编程，防止fd被滥用，导致的一些未定义行为
  if (elf == MAP_FAILED) {
    log_err("mmap() failed for %s", libpath);
    return exception_quit();
  }
  // FIXME：这里使用 mmap 的目的是为了获取到 libart.so 这个 elf 文件内各个符号的偏移量(基地址不同，但偏移量相同)

  this->load_addr_ptr = reinterpret_cast<void*>(load_addr); // so库加载到该进程中的基地址
  shoff = reinterpret_cast<int8_t*>(elf) + elf->e_shoff; // 节头表偏移量

  // 遍历节头表(Section Header Table)
  for (i = 0; i < elf->e_shnum; i++, shoff += elf->e_shentsize) {
    auto sh = reinterpret_cast<Elf_Shdr*>(shoff); // 节头
    log_dbg("%s: i=%d shdr=%p type=%x", __func__, i, sh, sh->sh_type);

    switch (sh->sh_type) {
    case SHT_DYNSYM: { // 符号表 .dynsym
      if (this->dynsym != nullptr) {
        log_err("%s: duplicate DYNSYM sections", libpath); /* .dynsym */
        return exception_quit();
      }
      this->dynsym = malloc(sh->sh_size);
      if (this->dynsym == nullptr) {
        log_err("%s: no memory for .dynsym", libpath);
        return exception_quit();
      }
      memcpy(this->dynsym, reinterpret_cast<int8_t*>(elf) + sh->sh_offset, sh->sh_size);
      this->nsyms = sh->sh_size / sizeof(Elf_Sym);
    }
    break;

    case SHT_STRTAB: { // 字符串(名字)表 .dynstr
      if (this->dynstr != nullptr) {
        break;    /* .dynstr is guaranteed to be the first STRTAB */
      }
      this->dynstr = malloc(sh->sh_size);
      if (this->dynstr == nullptr) {
        log_err("%s: no memory for .dynstr", libpath);
        return exception_quit();
      }
      memcpy(this->dynstr, reinterpret_cast<int8_t*>(elf) + sh->sh_offset, sh->sh_size);
    }
    break;

    case SHT_PROGBITS: {
      if (this->dynstr == nullptr || this->dynsym == nullptr) {
        break;
      }
      // won't even bother checking against the section name
      // - sh_addr: 该节在 elf 文件被加载到进程地址空间中后的偏移量，其在进程中的真实地址是:
      //            load_addr + sh->sh_addr.
      // - sh_offset 在 elf 文件中的偏移量 TODO: why ?
      this->bias = static_cast<off_t>(sh->sh_addr) - static_cast<off_t>(sh->sh_offset);
      i = elf->e_shnum;  /* exit for */
    }
    break;

    }
  }

  munmap(elf, size); // 释放elf在mmap空间的映射
  elf = nullptr;

  if (this->dynstr == nullptr || this->dynsym == nullptr) {
    log_err("dynamic sections not found in %s", libpath);
    return exception_quit();
  }

  log_dbg("%s: ok, dynsym = %p, dynstr = %p", libpath, this->dynsym, this->dynstr);
  return this->load_addr_ptr;
}

} // namespace iwatch
