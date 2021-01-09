//
// Created by 葛祥林 on 1/9/21.
//

#ifndef IWATCH_ARTRESTORE_H
#define IWATCH_ARTRESTORE_H

#include <vector>
#include <memory>
#include <string>

typedef struct {
  long backupArtmethodAddr;
  long artMethodAddr;
} ArtRestoreData;

class ArtRestore {
public:
  ArtRestore();
  virtual ~ArtRestore();

  void save(std::string className, std::string funcName, std::string desciptor,
            long backupArtmethodAddr, long artMethodAddr);

  void restoreArtMethod(std::string className, std::string funcName, std::string desciptor);

private:
  std::shared_ptr<std::vector<ArtRestoreData*>> restoreList;
};

#endif //IWATCH_ARTRESTORE_H
