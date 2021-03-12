//
// Created by 葛祥林 on 1/9/21.
//

#include "ArtRestore.h"
#include "iwatch_impl.h"

namespace iwatch {

// iwatch_impl.cpp
//extern size_t artMethodSizeV1;
//extern size_t artMethodSizeV2;

ArtRestore::ArtRestore() : restoreMap(), lock() {
}

ArtRestore::~ArtRestore() {
  // delete 掉 列表内存
  std::map<std::string, ArtRestoreData*>::iterator it;
  for (it = restoreMap.begin(); it != restoreMap.end(); ++it) {
    // 释放备份的原始方法
    delete[] reinterpret_cast<int8_t*>(it->second->backupArtmethodAddr);
    delete it->second;
    it->second = nullptr;
  }
  restoreMap.clear();
}

void ArtRestore::save(const std::string& className, const std::string& funcName,
                      const std::string& desciptor, long backupArtmethodAddr,
                      long artMethodAddr) {

  if (className.empty() || funcName.empty() || desciptor.empty()) {
    return;
  }

  auto data = new ArtRestoreData();
  data->backupArtmethodAddr = backupArtmethodAddr;
  data->artMethodAddr = artMethodAddr;

  // Java 层可能存在多线程竞态，需要互斥访问
  std::lock_guard<std::recursive_mutex> lockGuard(lock);
  // 比 insert() 好在：避免不必要的临时对象的产生
  restoreMap.emplace(std::make_pair(getKey(className, funcName, desciptor), data));
}

/**
 * 这里是恢复对应的 ArtMethod 为原始的，注意需要 delete 掉对应的堆内存，互斥访问
 */
void ArtRestore::restoreArtMethod(const std::string& className,
                                  const std::string& funcName,
                                  const std::string& desciptor) {

  std::string&& key = getKey(className, funcName, desciptor);
  restoreArtMethod(std::move(key));
}

void ArtRestore::restoreAllArtMethod() {
  size_t artMethodSize = getArtMethodSize();
  if (artMethodSize <= 0) {
    return;
  }

  std::lock_guard<std::recursive_mutex> lockGuard(lock);

  if (restoreMap.empty()) {
    return;
  }

  auto it = restoreMap.begin(); // std::map<std::string, ArtRestoreData*>::iterator
  while (it != restoreMap.end()) {
    doRestoreMethod(it->second->artMethodAddr, it->second->backupArtmethodAddr, artMethodSize);

    delete it->second;
    it = restoreMap.erase(it);
  }
}

void ArtRestore::restoreArtMethod(std::string&& key) {
  size_t artMethodSize = getArtMethodSize();
  if (artMethodSize <= 0) {
    return;
  }

  long artMethodAddr = 0L;
  long backupArtmethodAddr = 0L;

  { // 利用块作用域来及时释放锁，用以提高性能
    // Java 层可能存在多线程竞态，需要互斥访问
    std::lock_guard<std::recursive_mutex> lockGuard(lock);

    auto dataItor = restoreMap.find(key);
    if (dataItor == restoreMap.end()) {
      return;
    }
    artMethodAddr       = dataItor->second->artMethodAddr;
    backupArtmethodAddr = dataItor->second->backupArtmethodAddr;

    // 释放 + 删除
    delete dataItor->second;
    restoreMap.erase(dataItor);
  }

  doRestoreMethod(artMethodAddr, backupArtmethodAddr, artMethodSize);
}

void ArtRestore::doRestoreMethod(long artMethodAddr, long backupArtmethodAddr, size_t artMethodSize) {
  if (artMethodAddr <= 0L || backupArtmethodAddr <= 0L) {
    return;
  }

  try {
    void* srcArtMethodPtr = reinterpret_cast<void*>(artMethodAddr);
    void* backupArtMethod = reinterpret_cast<void*>(backupArtmethodAddr);
    memcpy(srcArtMethodPtr, backupArtMethod, artMethodSize);
    delete[] reinterpret_cast<int8_t*>(backupArtMethod); // 还原时卸载
  } catch (std::exception& e) {
    loge("ArtRestore::restoreArtMethod, eception: %s", e.what());
  }
}

/**
 * 判断该方法是否已经 hook 了，防止重复
 */
bool ArtRestore::inHooking(const std::string& className,
                           const std::string& funcName,
                           const std::string& desciptor) {

  std::string&& key = getKey(className, funcName, desciptor);

  std::lock_guard<std::recursive_mutex> lockGuard(lock);

  auto it = restoreMap.find(key);
  auto hooking = (it != restoreMap.end());
  return hooking;
}

} // namespace iwatch
