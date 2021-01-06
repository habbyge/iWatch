//
// Created by 葛祥林 on 12/22/20.
//

#ifndef IWATCH_ART_METHOD_11_H
#define IWATCH_ART_METHOD_11_H

#include <memory>
#include <stdint.h>
#include <atomic>
#include <string>

//#define DISALLOW_ALLOCATION() \
//  public: \
//    void operator delete(void*, size_t) { UNREACHABLE(); } \
//    void* operator new(size_t, void* ptr) noexcept { return ptr; } \
//    void operator delete(void*, void*) noexcept { } \
//  private: \
//    void* operator new(size_t) = delete  // NOLINT

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

class ClassLoader : public Object {
private:
  // Field order required by test "ValidateFieldOrderOfJavaCppUnionClasses".
  HeapReference<Object> packages_;
  HeapReference<ClassLoader> parent_;
  HeapReference<Object> proxyCache_;
  // Native pointer to class table, need to zero this out when image writing.
  uint32_t padding_;
  uint64_t allocator_;
  uint64_t class_table_;
};

class String final : public Object {
private:
  // Field order required by test "ValidateFieldOrderOfJavaCppUnionClasses".

  // If string compression is enabled, count_ holds the StringCompressionFlag in the
  // least significant bit and the length in the remaining bits, length = count_ >> 1.
  int32_t count_;

  uint32_t hash_code_;

  // Compression of all-ASCII into 8-bit memory leads to usage one of these fields
  union {
    uint16_t value_[0];
    uint8_t value_compressed_[0];
  };
};

class DexCache final : public Object {
private:
  HeapReference<ClassLoader> class_loader_;
  HeapReference<String> location_;

  uint64_t dex_file_;                // const DexFile*
  uint64_t preresolved_strings_;     // GcRoot<mirror::String*> array with num_preresolved_strings
                                     // elements.
  uint64_t resolved_call_sites_;     // GcRoot<CallSite>* array with num_resolved_call_sites_
                                     // elements.
  uint64_t resolved_fields_;         // std::atomic<FieldDexCachePair>*, array with
                                     // num_resolved_fields_ elements.
  uint64_t resolved_method_types_;   // std::atomic<MethodTypeDexCachePair>* array with
                                     // num_resolved_method_types_ elements.
  uint64_t resolved_methods_;        // ArtMethod*, array with num_resolved_methods_ elements.
  uint64_t resolved_types_;          // TypeDexCacheType*, array with num_resolved_types_ elements.
  uint64_t strings_;                 // std::atomic<StringDexCachePair>*, array with num_strings_
                                     // elements.

  uint32_t num_preresolved_strings_;    // Number of elements in the preresolved_strings_ array.
  uint32_t num_resolved_call_sites_;    // Number of elements in the call_sites_ array.
  uint32_t num_resolved_fields_;        // Number of elements in the resolved_fields_ array.
  uint32_t num_resolved_method_types_;  // Number of elements in the resolved_method_types_ array.
  uint32_t num_resolved_methods_;       // Number of elements in the resolved_methods_ array.
  uint32_t num_resolved_types_;         // Number of elements in the resolved_types_ array.
  uint32_t num_strings_;                // Number of elements in the strings_ array.
};

class Array : public Object {
private:
  // The number of array elements.
  // We only use the field indirectly using the LengthOffset() method.
  int32_t length_;
  // Marker for the data (used by generated code)
  // We only use the field indirectly using the DataOffset() method.
  uint32_t first_element_[0];
};

class PointerArray : public Array {
};

template<class T>
class ObjectArray: public Array {
};

class ClassExt : public Object {
private:
  // Field order required by test "ValidateFieldOrderOfJavaCppUnionClasses".
  // An array containing the jfieldIDs assigned to each field in the corresponding position in the
  // classes ifields_ array or '0' if no id has been assigned to that field yet.
  HeapReference<PointerArray> instance_jfield_ids_;

  // An array containing the jmethodIDs assigned to each method in the corresponding position in
  // the classes methods_ array or '0' if no id has been assigned to that method yet.
  HeapReference<PointerArray> jmethod_ids_;

  // If set this is the Class object that was being used before a structural redefinition occurred.
  HeapReference<Class> obsolete_class_;

  HeapReference<ObjectArray<DexCache>> obsolete_dex_caches_;

  HeapReference<PointerArray> obsolete_methods_;

  HeapReference<Object> original_dex_file_;

  // An array containing the jfieldIDs assigned to each field in the corresponding position in the
  // classes sfields_ array or '0' if no id has been assigned to that field yet.
  HeapReference<PointerArray> static_jfield_ids_;

  // The saved verification error of this class.
  HeapReference<Object> verify_error_;

  // Native pointer to DexFile and ClassDef index of this class before it was JVMTI-redefined.
  int64_t pre_redefine_dex_file_ptr_;
  int32_t pre_redefine_class_def_index_;
};

class IfTable final : public ObjectArray<Object> {
};

class Class final : public mirror::Object {
private:
  // 'Class' Object Fields
  // Order governed by java field ordering. See art::ClassLinker::LinkFields.

  // Defining class loader, or null for the "bootstrap" system loader.
  HeapReference<ClassLoader> class_loader_;

  // For array classes, the component class object for instanceof/checkcast
  // (for String[][][], this will be String[][]). null for non-array classes.
  HeapReference<Class> component_type_;

  // DexCache of resolved constant pool entries (will be null for classes generated by the
  // runtime such as arrays and primitive classes).
  HeapReference<DexCache> dex_cache_;

  // Extraneous class data that is not always needed. This field is allocated lazily and may
  // only be set with 'this' locked. This is synchronized on 'this'.
  // TODO(allight) We should probably synchronize it on something external or handle allocation in
  // some other (safe) way to prevent possible deadlocks.
  HeapReference<ClassExt> ext_data_;

  // The interface table (iftable_) contains pairs of a interface class and an array of the
  // interface methods. There is one pair per interface supported by this class.  That means one
  // pair for each interface we support directly, indirectly via superclass, or indirectly via a
  // superinterface.  This will be null if neither we nor our superclass implement any interfaces.
  //
  // Why we need this: given "class Foo implements Face", declare "Face faceObj = new Foo()".
  // Invoke faceObj.blah(), where "blah" is part of the Face interface.  We can't easily use a
  // single vtable.
  //
  // For every interface a concrete class implements, we create an array of the concrete vtable_
  // methods for the methods in the interface.
  HeapReference<IfTable> iftable_;

  // Descriptor for the class such as "java.lang.Class" or "[C". Lazily initialized by ComputeName
  HeapReference<String> name_;

  // The superclass, or null if this is java.lang.Object or a primitive type.
  //
  // Note that interfaces have java.lang.Object as their
  // superclass. This doesn't match the expectations in JNI
  // GetSuperClass or java.lang.Class.getSuperClass() which need to
  // check for interfaces and return null.
  HeapReference<Class> super_class_;

  // Virtual method table (vtable), for use by "invoke-virtual".  The vtable from the superclass is
  // copied in, and virtual methods from our class either replace those from the super or are
  // appended. For abstract classes, methods may be created in the vtable that aren't in
  // virtual_ methods_ for miranda methods.
  HeapReference<PointerArray> vtable_;

  // instance fields
  //
  // These describe the layout of the contents of an Object.
  // Note that only the fields directly declared by this class are
  // listed in ifields; fields declared by a superclass are listed in
  // the superclass's Class.ifields.
  //
  // ArtFields are allocated as a length prefixed ArtField array, and not an array of pointers to
  // ArtFields.
  uint64_t ifields_;

  // Pointer to an ArtMethod length-prefixed array. All the methods where this class is the place
  // where they are logically defined. This includes all private, static, final and virtual methods
  // as well as inherited default methods and miranda methods.
  //
  // The slice methods_ [0, virtual_methods_offset_) are the direct (static, private, init) methods
  // declared by this class.
  //
  // The slice methods_ [virtual_methods_offset_, copied_methods_offset_) are the virtual methods
  // declared by this class.
  //
  // The slice methods_ [copied_methods_offset_, |methods_|) are the methods that are copied from
  // interfaces such as miranda or default methods. These are copied for resolution purposes as this
  // class is where they are (logically) declared as far as the virtual dispatch is concerned.
  //
  // Note that this field is used by the native debugger as the unique identifier for the type.
  // FiXME: 这里是存储该 Class 中的所有包含的 ArtMethod 对象，包括：virtual、satic、final以及集成的方法，
  //  从这个字段的初始化可以看出，ArtMethod 对象是按照从上到下顺序依次紧挨着存储的，所以 iWwatch 项目中的计算
  //  同一个 class 中两个相邻的方法相减，是可以计算出 ArtMethod 大小的. Android-11 中的 index 索引其实就是
  //  这里每一个 ArtMethod 对象在 这个字段中排列的顺序.
  uint64_t methods_;

  // Static fields length-prefixed array.
  uint64_t sfields_;

  // Access flags; low 16 bits are defined by VM spec.
  uint32_t access_flags_;

  // Class flags to help speed up visiting object references.
  uint32_t class_flags_;

  // Total size of the Class instance; used when allocating storage on gc heap.
  // See also object_size_.
  uint32_t class_size_;

  // Tid used to check for recursive <clinit> invocation.
  pid_t clinit_thread_id_;
  static_assert(sizeof(pid_t) == sizeof(int32_t), "java.lang.Class.clinitThreadId size check");

  // ClassDef index in dex file, -1 if no class definition such as an array.
  // TODO: really 16bits
  int32_t dex_class_def_idx_;

  // Type index in dex file.
  // TODO: really 16bits
  int32_t dex_type_idx_;

  // Number of instance fields that are object refs.
  uint32_t num_reference_instance_fields_;

  // Number of static fields that are object refs,
  uint32_t num_reference_static_fields_;

  // Total object size; used when allocating storage on gc heap.
  // (For interfaces and abstract classes this will be zero.)
  // See also class_size_.
  uint32_t object_size_;

  // Aligned object size for allocation fast path. The value is max uint32_t if the object is
  // uninitialized or finalizable. Not currently used for variable sized objects.
  uint32_t object_size_alloc_fast_path_;

  // The lower 16 bits contains a Primitive::Type value. The upper 16
  // bits contains the size shift of the primitive type.
  uint32_t primitive_type_;

  // Bitmap of offsets of ifields.
  uint32_t reference_instance_offsets_;

  // See the real definition in subtype_check_bits_and_status.h
  // typeof(status_) is actually SubtypeCheckBitsAndStatus.
  uint32_t status_;

  // The offset of the first virtual method that is copied from an interface. This includes miranda,
  // default, and default-conflict methods. Having a hard limit of ((2 << 16) - 1) for methods
  // defined on a single class is well established in Java so we will use only uint16_t's here.
  uint16_t copied_methods_offset_;

  // The offset of the first declared virtual methods in the methods_ array.
  uint16_t virtual_methods_offset_;

  // TODO: ?
  // initiating class loader list
  // NOTE: for classes with low serialNumber, these are unused, and the
  // values are kept in a table in gDvm.
  // InitiatingLoaderList initiating_loader_list_;

  // The following data exist in real class objects.
  // Embedded Imtable, for class object that's not an interface, fixed size.
  // ImTableEntry embedded_imtable_[0];
  // Embedded Vtable, for class object that's not an interface, variable size.
  // VTableEntry embedded_vtable_[0];
  // Static fields, variable size.
  // uint32_t fields_[0];
};

class ArtMethod_11 final { // 静态成员变量，不占用类地址空间
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

template<typename MirrorType>
class ObjPtr {
public:
  ObjPtr(jobject obj) {
  }
private:
  // The encoded reference and cookie.
  uintptr_t reference_;
};

} // art

#endif //IWATCH_ART_METHOD_11_H
