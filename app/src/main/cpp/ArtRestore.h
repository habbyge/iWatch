//
// Created by 葛祥林 on 1/9/21.
//

#ifndef IWATCH_ARTRESTORE_H
#define IWATCH_ARTRESTORE_H

//#include <memory>
#include <string>
#include <mutex>
#include <chrono>
#include <map>

#include "common/log.h"

typedef struct {
  long backupArtmethodAddr;
  long artMethodAddr;
} ArtRestoreData;

class ArtRestore final {
public:
  explicit ArtRestore();
  virtual ~ArtRestore();

  ArtRestore(const ArtRestore&) = delete;
  ArtRestore& operator=(const ArtRestore&) = delete;

  void save(std::string& className, std::string& funcName, std::string& desciptor,
            long backupArtmethodAddr, long artMethodAddr);

  void restoreArtMethod(std::string& className, std::string& funcName, std::string& desciptor); // TODO:
  void restoreAllArtMethod(); // TODO:

  bool inHooking(std::string& className, std::string& funcName, std::string& desciptor); // TODO: ing

private:
  // 这里可能存在并发(来自Java层)，需要互斥访问.
  // 这里不能使用 C++11 的 智能指针，因为 restoreMap 以及 restoreMap 中的成员都需要根据恢复逻辑手工释放.
  // 即: 只有在需要恢复原始方法时、且恢复完成后，才能释放其 ArtRestoreData* 指向的对象，所以需要手工释放.
  /*std::shared_ptr<std::vector<ArtRestoreData*>> restoreList;*/
  std::map<std::string, ArtRestoreData*> restoreMap;
  std::recursive_mutex lock;

  void restoreArtMethod(std::string&& key);
  static void doRestoreMethod(long artMethodAddr, long backupArtmethodAddr, size_t artMethodSize);

  static inline size_t getArtMethodSize();

  inline std::string&& getKey(std::string& className, std::string& funcName, std::string& desciptor) {
    return std::move(className + "$" + funcName + "$" + desciptor);
  }
};

#endif // IWATCH_ARTRESTORE_H
