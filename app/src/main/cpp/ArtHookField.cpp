//
// Created by 葛祥林 on 1/26/21.
//

#include "ArtHookField.h"
#include "iwatch_impl.h"

namespace iwatch {

void ArtHookField::initArtField(JNIEnv*, const std::shared_ptr<Elf>& elf_op) {
  /*DecodeFieldId = reinterpret_cast<DecodeFieldId_t>(elf_op->dlsym_elf(DecodeFieldId_Sym));
  logi("initArtField, DecodeFieldId=%p", DecodeFieldId);*/

  FindFieldJNI = reinterpret_cast<FindFieldJNI_t>(elf_op->dlsym_elf(FindFieldJNI_Sym));
  logi("initArtField, FindFieldJNI=%p", FindFieldJNI);
}

/*void* ArtHookField::getArtField(JNIEnv* env, jfieldID fieldId) {
  try {
    void* artFieldAddr = DecodeFieldId(fieldId); // 返回 ArtField*
    logd("getArtField(DecodeFieldId): artFieldAddr=%p", artFieldAddr);
    return artFieldAddr;
  } catch (std::exception& e) {
    loge("getArtField, DecodeFieldId, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}*/

/**
 * 查代码：jni.h中的FromReflectedField()实现，对应是：art/runtime/jni/jni_internal.cc 中:
 * static jfieldID FromReflectedField(JNIEnv* env, jobject jlr_field)
 * 本质是是从 mirror::Field.field->GetArtField() 返回 ArtFile* 地址，如果kEnableIndexIds=true的话，
 * 这里返回的是其在Class中的fields_中的偏移量索引，因此不行，只能使用：
 * ArtField* FindFieldJNI(const ScopedObjectAccess& soa,
 *                        jclass jni_class,
 *                        const char* name,
 *                        const char* sig,
 *                        bool is_static)
 */
void* ArtHookField::getArtField(JNIEnv* env, jclass jni_class, const char* name, const char* sig, bool isStatic) {
  try {
    art::ScopedObjectAccess soa{env};
    void* addr = FindFieldJNI(soa, jni_class, name, sig, isStatic); // 返回 ArtField*
    logd("getArtField(FindFieldJNI): name=%s, desc=%s, static=%d, addr=%p", name, sig, isStatic, addr);
    return addr;
  } catch (std::exception& e) {
    loge("getArtField, FindFieldJNI, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}

void ArtHookField::addAccessFlagsPublic(void* artField) {
  // 从 Android5.0 ~ Android11.0 的版本中，ArtField 中 access_flags_ 字段偏移量都是1
  uint32_t* access_flags_addr = reinterpret_cast<uint32_t*>(artField) + 1;
  uint32_t access_flags_ = *access_flags_addr;
  if ((access_flags_ & kAccSynthetic) == kAccSynthetic) {
    logd("addAccessFlagsPublic, Synthetic: access_flags_ptr=%p, access_flags=%u -> %u",
         access_flags_addr, access_flags_, *access_flags_addr);
    return; // 由于内部类合成的字段(例如：外部类的对象字段)，不能设置其访问权限
  }

  *access_flags_addr = ((*access_flags_addr) & (~kAccPrivate) & (~kAccProtected)) | kAccPublic;

  logw("addAccessFlagsPublic, access_flags_ptr=%p, access_flags=%u -> %u",
       access_flags_addr, access_flags_, *access_flags_addr);
}

} // namespace iwatch
