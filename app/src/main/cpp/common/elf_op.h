//
// Created by 葛祥林 on 1/21/21.
//

/**
 * 这里的 ELF(so库文件) 解析符号的方案，稍微修改就能实现对 Native(C/C++) 函数的 Hook.
 */

#pragma once

#ifndef IWATCH_ELF_OP_H
#define IWATCH_ELF_OP_H

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

namespace iwatch {

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

#ifdef __cplusplus
extern "C" {
#endif

class Elf final {
public:
  Elf() : load_addr_ptr(nullptr), dynstr(nullptr), dynsym(nullptr), nsyms(0), bias(0) {}

  virtual ~Elf() {
    dlclose_elf();
  }

  void* dlopen_elf(const char* filename, int flags);
  void* dlsym_elf(const char* symbol_name);
  int dlclose_elf();
  const char* dlerror_elf();

  inline void* getLoadAddr() const {
    return load_addr_ptr;
  }
  
private:
  void* load_addr_ptr; // so库文件加载到进程中的基地址，来自 /proc/pid/maps，这里无需释放 just an address

  void* dynstr; // 名称字符串表(Section)

  void* dynsym;       // 符号表(Section)
  size_t nsyms; // 符号表中的符号item条数

  off_t bias; // 是节头表在进程地址空间中的基地址(偏移地址) // TODO: why ?

  void* initElf(const char* libpath);
  int _dlclose();
  void* _dlopen(const char* filename, int flags);
  void* _dlsym(const char* symbol_name);
  int get_sdk_level();
  static const char* _dlerror();

  int SDK_INT;
};

#ifdef __cplusplus
}
#endif

} // namespace iwatch

#endif //IWATCH_ELF_OP_H
