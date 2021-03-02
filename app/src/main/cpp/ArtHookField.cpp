//
// Created by 葛祥林 on 1/26/21.
//

#include "ArtHookField.h"

namespace iwatch {

void* ArtHookField::getArtField(JNIEnv* env, jobject field) {
  return nullptr; // todo
}

void* ArtHookField::getArtField(JNIEnv* env, jclass java_class, const char* name, const char* sig, bool is_static) {
  return nullptr; // todo
}

} // namespace iwatch
