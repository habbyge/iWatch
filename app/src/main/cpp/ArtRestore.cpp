//
// Created by 葛祥林 on 1/9/21.
//

#include "ArtRestore.h"

ArtRestore::ArtRestore() {
  restoreList = std::make_shared_ptr<>
}

ArtRestore::~ArtRestore() {
  // TODO: ing...... delete 掉 列表内存
}

void ArtRestore::save(std::string className, std::string funcName, std::string desciptor,
                      long backupArtmethodAddr, long artMethodAddr) {

  // TODO: 需要互斥访问
  auto data = new ArtRestoreData();
  data->backupArtmethodAddr = backupArtmethodAddr;
  data->artMethodAddr = artMethodAddr;
  restoreList->push_back(data);
}

/**
 * TODO: 这里是恢复对应的 ArtMethod 为原始的，注意需要 delete 掉对应的堆内存，互斥访问
 */
void ArtRestore::restoreArtMethod(std::string className, std::string funcName, std::string desciptor) {
  // todo ing......
}
