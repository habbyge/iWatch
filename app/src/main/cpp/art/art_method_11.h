//
// Created by 葛祥林 on 12/22/20.
//

#ifndef IWATCH_ART_METHOD_11_H
#define IWATCH_ART_METHOD_11_H

#include <memory>
#include <stdint.h>
#include <atomic>

class Class; // 声明

namespace art {

enum class PointerSize : size_t {
  k32 = 4,
  k64 = 8
};

template<class MirrorType>
class GcRoot {
private:
  uint32_t reference_;
};

namespace mirror {

template<class MirrorType>
class HeapReference {
private:
  // The encoded reference to a mirror::Object. Atomically updateable.
  std::atomic<uint32_t> reference_;
};

class Object {
public:
  // The number of vtable entries in java.lang.Object.
  static constexpr size_t kVTableLength = 11;

  // The size of the java.lang.Class representing a java.lang.Object.
  static uint32_t ClassSize(PointerSize pointer_size);

private:
  static std::atomic<uint32_t> hash_code_seed;

  // The Class representing the type of the object.
  HeapReference<Class> klass_;

  // Monitor and hash code information.
  uint32_t monitor_;

#ifdef USE_BROOKS_READ_BARRIER
  // Note names use a 'x' prefix and the x_rb_ptr_ is of type int
  // instead of Object to go with the alphabetical/by-type field order
  // on the Java side.
  uint32_t x_rb_ptr_;      // For the Brooks pointer.
  uint32_t x_xpadding_;    // For 8-byte alignment.
#endif
};

class Class final : public mirror::Object {
  // TODO: ing
};

class art_method_11 final { // 静态成员变量，不占用类地址空间
protected:
  // Field order required by test "ValidateFieldOrderOfJavaCppUnionClasses".
  // The class we are a part of.
  GcRoot<mirror::Class> declaring_class_;

  // Access flags; low 16 bits are defined by spec.
  // Getting and setting this flag needs to be atomic when concurrency is
  // possible, e.g. after this method's class is linked. Such as when setting
  // verifier flags and single-implementation flag.
  std::atomic<std::uint32_t> access_flags_;

  /* Dex file fields. The defining dex file is available via declaring_class_->dex_cache_ */

  // Offset to the CodeItem.
  uint32_t dex_code_item_offset_;

  // Index into method_ids of the dex file associated with this method.
  uint32_t dex_method_index_;

  /* End of dex file fields. */

  // Entry within a dispatch table for this method. For static/direct methods the index is into
  // the declaringClass.directMethods, for virtual methods the vtable and for interface methods the
  // ifTable.
  uint16_t method_index_;

  union {
    // Non-abstract methods: The hotness we measure for this method. Not atomic,
    // as we allow missing increments: if the method is hot, we will see it eventually.
    uint16_t hotness_count_;
    // Abstract methods: IMT index (bitwise negated) or zero if it was not cached.
    // The negation is needed to distinguish zero index and missing cached entry.
    uint16_t imt_index_;
  };

  // Fake padding field gets inserted here.

  // Must be the last fields in the method.
  struct PtrSizedFields {
    // Depending on the method type, the data is
    //   - native method: pointer to the JNI function registered to this method
    //                    or a function to resolve the JNI function,
    //   - conflict method: ImtConflictTable,
    //   - abstract/interface method: the single-implementation if any,
    //   - proxy method: the original interface method or constructor,
    //   - other methods: the profiling data.
    void* data_;

    // Method dispatch from quick compiled code invokes this pointer which may cause bridging into
    // the interpreter.
    void* entry_point_from_quick_compiled_code_;
  } ptr_sized_fields_;
};

} // mirror

} // art

#endif //IWATCH_ART_METHOD_11_H