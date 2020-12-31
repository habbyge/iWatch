//
// Created by 葛祥林 on 12/22/20.
//

#ifndef COMMON_ELF_OP_H
#define COMMON_ELF_OP_H

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <elf.h>
#include <dlfcn.h>
#include <sys/system_properties.h>
#include "log.h"

#ifdef __LP64__
  #define Elf_Ehdr Elf64_Ehdr
  #define Elf_Shdr Elf64_Shdr
  #define Elf_Sym  Elf64_Sym
#else
  #define Elf_Ehdr Elf32_Ehdr
  #define Elf_Shdr Elf32_Shdr
  #define Elf_Sym  Elf32_Sym
#endif

#if defined(__LP64__)
  static const char* const kSystemLibDir = "/system/lib64/";
  static const char* const kOdmLibDir = "/odm/lib64/";
  static const char* const kVendorLibDir = "/vendor/lib64/";
  static const char* const kApexLibDir = "/apex/com.android.runtime/lib64/"; // Android-10
  static const char* const kApexLibDir_11 = "/apex/com.android.art/lib64/"; // Android-11
#else
  static const char* const kSystemLibDir = "/system/lib/";
  static const char* const kOdmLibDir = "/odm/lib/";
  static const char* const kVendorLibDir = "/vendor/lib/";
  static const char* const kApexLibDir = "/apex/com.android.runtime/lib/"; // // Android-10
  static const char* const kApexLibDir_11 = "/apex/com.android.art/lib/"; // Android-11
#endif

// 定义需要获取的符号
using

/**
 * 加载 Elf 文件到mmap后，解析后的结果
 */
typedef struct elf_ctx {
  void* load_addr; // so库文件加载到进程中的基地址，来自 /proc/pid/maps

  void* dynstr;    // 名称字符串表(Section)

  void* dynsym;    // 符号表(Section)
  unsigned int nsyms;       // 符号表中的符号item条数

  off_t bias;      // 是节头表在进程地址空间中的基地址(偏移地址) TODO:
} elf_ctx_t;

#ifdef __cplusplus
extern "C" {
#endif

__unused
void* dlopen_elf(const char* filename, int flags);
__unused
void* dlsym_elf(void* handle, const char* symbol);
__unused
int dlclose_elf(void* handle);
__unused
const char* dlerror_elf();

#ifdef __cplusplus
}
#endif

#endif // COMMON_ELF_OP_H
