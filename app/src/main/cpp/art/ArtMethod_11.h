//
// Created by 葛祥林 on 12/22/20.
//

#ifndef IWATCH_ARTMETHOD_11_H
#define IWATCH_ARTMETHOD_11_H

#include <memory>
#include <stdint.h>

namespace art {

namespace mirror {

class ArtMethod_11 final { // 静态成员变量，不占用类地址空间
protected:
    int* declaring_class_;
    std::atomic<uint32_t> access_flags_;
    uint32_t dex_code_item_offset_;
    uint32_t dex_method_index_;
    uint16_t method_index_;

    union {
        uint16_t hotness_count_;
        uint16_t imt_index_;
    };

    struct PtrSizedFields {
        void* data_;
        void* entry_point_from_quick_compiled_code_;
    } ptr_sized_fields_;
};

} // mirror

} // art

#endif //IWATCH_ARTMETHOD_11_H
