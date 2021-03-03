//
// Created by 葛祥林 on 1/26/21.
//

#include "ArtHookField.h"
#include "iwatch_impl.h"

namespace iwatch {

void ArtHookField::initArtField(JNIEnv*, const std::shared_ptr<Elf>& elf_op) {
  FindFieldJNI = reinterpret_cast<FindFieldJNI_t>(elf_op->dlsym_elf(FindFieldJNI_Sym));
  logi("initArtField, FindFieldJNI=%p", FindFieldJNI);
}

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
void* ArtHookField::getArtField(JNIEnv* env, jclass jni_class, const char* name,
                                const char* sig, bool isStatic) {

  try {
    art::ScopedObjectAccess soa(env);
    return FindFieldJNI(soa, jni_class, name, sig, isStatic); // 返回 ArtField*
  } catch (std::exception& e) {
    loge("getArtField, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}

} // namespace iwatch
