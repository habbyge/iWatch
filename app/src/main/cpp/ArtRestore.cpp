//
// Created by 葛祥林 on 1/9/21.
//

#include "ArtRestore.h"

// iwatch_impl.cpp
extern size_t artMethodSizeV1;
extern size_t artMethodSizeV2;

ArtRestore::ArtRestore() : lock(std::move(std::timed_mutex())),
                           restoreMap(std::move(std::map<std::string&&, ArtRestoreData*>())) {
}

ArtRestore::~ArtRestore() {
  // delete 掉 列表内存
  std::map<std::string&&, ArtRestoreData*>::iterator it;
  for (it = restoreMap.begin(); it != restoreMap.end(); ++it) {
    delete it->second;
    it->second = nullptr;
  }
}

void ArtRestore::save(std::string className, std::string funcName, std::string desciptor,
                      long backupArtmethodAddr, long artMethodAddr) {

  if (className.empty() || funcName.empty() || desciptor.empty()) {
    return;
  }

  auto data = new ArtRestoreData();
  data->backupArtmethodAddr = backupArtmethodAddr;
  data->artMethodAddr = artMethodAddr;

  // Java 层可能存在多线程竞态，需要互斥访问
  while (!lock.try_lock_for(std::chrono::milliseconds(1000))) {
    loge("ArtRestore::save, className=%s, func=%s, desc=%s",
        className.c_str(), funcName.c_str(), desciptor.c_str());
  }

  // 比 insert() 好在：避免不必要的临时对象的产生
  restoreMap.emplace(std::make_pair(className + "$" + funcName + "$" + desciptor, data));

  lock.unlock();
}

/**
 * 这里是恢复对应的 ArtMethod 为原始的，注意需要 delete 掉对应的堆内存，互斥访问
 */
void ArtRestore::restoreArtMethod(std::string className, std::string funcName, std::string desciptor) {
  int artMethodSize = artMethodSizeV1 <= 0 ? artMethodSizeV2 : artMethodSizeV1;
  if (artMethodSize <= 0) {
    return;
  }

  std::string key = className + "$" + funcName + "$" + desciptor;

  // Java 层可能存在多线程竞态，需要互斥访问
  while (!lock.try_lock_for(std::chrono::milliseconds(1000))) {
    loge("ArtRestore::save, className=%s, func=%s, desc=%s",
         className.c_str(), funcName.c_str(), desciptor.c_str());
  }
  auto dataItor = restoreMap.find(key);
  lock.unlock();

  if (dataItor == restoreMap.end()) {
    return;
  }

  if (dataItor->second->artMethodAddr <= 0L) {
    return;
  }
  if (dataItor->second->backupArtmethodAddr <= 0L) {
    return;
  }

  try {
    void* srcArtMethodPtr = reinterpret_cast<void*>(dataItor->second->artMethodAddr);
    void* backupArtMethod = reinterpret_cast<void*>(dataItor->second->backupArtmethodAddr);
    memcpy(srcArtMethodPtr, backupArtMethod, artMethodSize);
    delete[] reinterpret_cast<int8_t*>(backupArtMethod); // 还原时卸载
  } catch (std::exception& e) {
    loge("ArtRestore::restoreArtMethod, eception: %s", e.what());
  }
}
