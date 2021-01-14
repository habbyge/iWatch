//
// Created by 葛祥林 on 1/9/21.
//

#include "ArtRestore.h"
#include "iwatch_impl.h"

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

void ArtRestore::save(std::string& className, std::string& funcName, std::string& desciptor,
                      long backupArtmethodAddr, long artMethodAddr) {

  if (className.empty() || funcName.empty() || desciptor.empty()) {
    return;
  }

  auto data = new ArtRestoreData();
  data->backupArtmethodAddr = backupArtmethodAddr;
  data->artMethodAddr = artMethodAddr;

  // Java 层可能存在多线程竞态，需要互斥访问
  tryLock();
  // 比 insert() 好在：避免不必要的临时对象的产生
  restoreMap.emplace(std::make_pair(getKey(className, funcName, desciptor), data));

  unlock();
}

/**
 * 这里是恢复对应的 ArtMethod 为原始的，注意需要 delete 掉对应的堆内存，互斥访问
 */
void ArtRestore::restoreArtMethod(std::string& className, std::string& funcName, std::string& desciptor) {
  std::string&& key = getKey(className, funcName, desciptor);
  restoreArtMethod(std::move(key));
}

void ArtRestore::restoreAllArtMethod() {
  size_t artMethodSize = getArtMethodSize();
  if (artMethodSize <= 0) {
    return;
  }

  tryLock();

  auto it = restoreMap.begin(); // std::map<std::string, ArtRestoreData*>::iterator
  while (it != restoreMap.end()) {
    doRestoreMethod(it->second->artMethodAddr, it->second->backupArtmethodAddr, artMethodSize);

    delete it->second;
    it = restoreMap.erase(it);
  }

  unlock();
}

void ArtRestore::restoreArtMethod(std::string&& key) {
  size_t artMethodSize = getArtMethodSize();
  if (artMethodSize <= 0) {
    return;
  }

  // Java 层可能存在多线程竞态，需要互斥访问
  tryLock();

  auto dataItor = restoreMap.find(key);
  if (dataItor == restoreMap.end()) {
    unlock();
    return;
  }
  long artMethodAddr = dataItor->second->artMethodAddr;
  long backupArtmethodAddr = dataItor->second->backupArtmethodAddr;

  // 释放 + 删除
  delete dataItor->second;
  restoreMap.erase(dataItor);

  unlock();

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
bool ArtRestore::inHooking(std::string& className, std::string& funcName, std::string& desciptor) {
  std::string&& key = getKey(className, funcName, desciptor);

  tryLock();
  auto it = restoreMap.find(key);
  auto hooking = (it != restoreMap.end());
  unlock();
  return hooking;
}

inline size_t ArtRestore::getArtMethodSize() {
  return artMethodSizeV1 <= 0 ? artMethodSizeV2 : artMethodSizeV1;
}
