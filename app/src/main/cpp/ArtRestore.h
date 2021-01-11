//
// Created by 葛祥林 on 1/9/21.
//

#ifndef IWATCH_ARTRESTORE_H
#define IWATCH_ARTRESTORE_H

#include <map>
/*#include <memory>*/
#include <string>
#include <mutex>
#include <chrono>
#include "common/log.h"

typedef struct {
  long backupArtmethodAddr;
  long artMethodAddr;
} ArtRestoreData;

class ArtRestore {
public:
  explicit ArtRestore();
  virtual ~ArtRestore();

  void save(std::string className, std::string funcName, std::string desciptor,
            long backupArtmethodAddr, long artMethodAddr);

  void restoreArtMethod(std::string className, std::string funcName, std::string desciptor);

private:
  // 这里可能存在并发，需要互斥访问.
  // 这里不能使用 C++11 的 智能指针，因为map以及map中的成员都需要根据逻辑手工释放.
  /*std::shared_ptr<std::vector<ArtRestoreData*>> restoreList;*/
  std::map<std::string&&, ArtRestoreData*>&& restoreMap;
  std::timed_mutex&& lock;
};

#endif // IWATCH_ARTRESTORE_H
