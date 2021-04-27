//
// Created by 葛祥林 on 1/9/21.
//

#pragma once

#ifndef IWATCH_ARTRESTORE_H
#define IWATCH_ARTRESTORE_H

//#include <memory>
#include <string>
#include <mutex>
#include <chrono>
#include <map>
#include <stdint.h>

#include "common/log.h"

namespace iwatch {

typedef struct {
  uintptr_t backupArtmethodAddr;
  uintptr_t artMethodAddr;
} ArtRestoreData;

class ArtRestore final {
public:
  explicit ArtRestore();
  virtual ~ArtRestore();

  ArtRestore(const ArtRestore&) = delete;
  ArtRestore& operator=(const ArtRestore&) = delete;

  void save(const std::string& className, const std::string& funcName,
            const std::string& desciptor, uintptr_t backupArtmethodAddr,
            uintptr_t artMethodAddr);

  void restoreArtMethod(const std::string& className,
                        const std::string& funcName,
                        const std::string& desciptor);

  void restoreAllArtMethod();

  bool inHooking(const std::string& className, const std::string& funcName, const std::string& desciptor);

private:
  // 这里可能存在并发(来自Java层)，需要互斥访问.
  // 这里不使用 C++11 的 智能指针，因为 restoreMap 以及 restoreMap 中的成员都需要根据恢复逻辑手工释放.
  // 即: 只有在需要恢复原始方法时、且恢复完成后，才能释放其 ArtRestoreData* 指向的对象，所以需要手工释放，进行精确释放.
  /*std::shared_ptr<std::vector<ArtRestoreData*>> restoreList;*/
  std::map<std::string, ArtRestoreData*> restoreMap;
  std::recursive_mutex lock;

  void restoreArtMethod(std::string&& key);
  static void doRestoreMethod(uintptr_t artMethodAddr, uintptr_t backupArtmethodAddr, size_t artMethodSize);

  inline std::string getKey(const std::string& className,
                            const std::string& funcName,
                            const std::string& desciptor) const noexcept {

    return className + "$" + funcName + "$" + desciptor;
  }
};

} // namespace iwatch

#endif // IWATCH_ARTRESTORE_H
