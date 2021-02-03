//
// Created by 葛祥林 on 2020/11/25.
//

#pragma once

#ifndef METHODHOOK_LOG_H
#define METHODHOOK_LOG_H

#include <android/log.h>

namespace iwatch {

#define TAG "iWatch"

#define LOG_DBG

#ifdef LOG_DBG
#define log_info(fmt, args...) __android_log_print(ANDROID_LOG_INFO, \
                                                       TAG,         \
                                                       (const char*) fmt, ##args)

#define log_err(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, \
                                                      TAG,          \
                                                      (const char *) fmt, ##args)
#define log_dbg log_info

#define logv(...)  ((void)__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__))
#define logi(...)  ((void)__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))
#define logd(...)  ((void)__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__))
#define logw(...)  ((void)__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define loge(...)  ((void)__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))

#else
#define log_dbg(...)
#define log_info(fmt, args...)
#define log_err(fmt, args...)
#endif

} // namespace iwatch

#endif //METHODHOOK_LOG_H
