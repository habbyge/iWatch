////
//// Created by 葛祥林 on 12/14/20.
////
//
//#ifndef IWATCH_ARTMETHOD_H
//#define IWATCH_ARTMETHOD_H
//
//#include <stdint.h>
//#include <atomic>
//
//class ArtMethod final {
//    static constexpr uint32_t kRuntimeMethodDexMethodIndex = 0xFFFFFFFF;
//    // Field order required by test "ValidateFieldOrderOfJavaCppUnionClasses".
//    // The class we are a part of.
//    int* declaring_class_;
//
//    std::atomic<std::uint32_t> access_flags_;
//    uint32_t dex_code_item_offset_;
//    uint32_t dex_method_index_;
//    uint16_t method_index_;
//
//    union {
//        // Non-abstract methods: The hotness we measure for this method. Not atomic,
//        // as we allow missing increments: if the method is hot, we will see it eventually.
//        uint16_t hotness_count_;
//        // Abstract methods: IMT index (bitwise negated) or zero if it was not cached.
//        // The negation is needed to distinguish zero index and missing cached entry.
//        uint16_t imt_index_;
//    };
//
//    struct PtrSizedFields {
//        // Depending on the method type, the data is
//        //   - native method: pointer to the JNI function registered to this method
//        //                    or a function to resolve the JNI function,
//        //   - conflict method: ImtConflictTable,
//        //   - abstract/interface method: the single-implementation if any,
//        //   - proxy method: the original interface method or constructor,
//        //   - other methods: the profiling data.
//        void* data_;
//
//        // Method dispatch from quick compiled code invokes this pointer which may cause bridging into
//        // the interpreter.
//        void* entry_point_from_quick_compiled_code_;
//    } ptr_sized_fields_;
//};
//
//#endif //IWATCH_ARTMETHOD_H
