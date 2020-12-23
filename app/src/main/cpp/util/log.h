//
// Created by 葛祥林 on 2020/11/25.
//

#ifndef METHODHOOK_LOG_H
#define METHODHOOK_LOG_H

#include <android/log.h>

#define logv(...)  ((void)__android_log_print(ANDROID_LOG_VERBOSE, "iWatch", __VA_ARGS__))
#define logi(...)  ((void)__android_log_print(ANDROID_LOG_INFO, "iWatch", __VA_ARGS__))
#define logd(...)  ((void)__android_log_print(ANDROID_LOG_DEBUG, "iWatch", __VA_ARGS__))
#define loge(...)  ((void)__android_log_print(ANDROID_LOG_ERROR, "iWatch", __VA_ARGS__))

#endif //METHODHOOK_LOG_H
