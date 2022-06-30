#ifndef ANDROIDOPUSENCODER_LOGGER_H
#define ANDROIDOPUSENCODER_LOGGER_H
#include <android/log.h>


#define LOGE(tag, ...) __android_log_print(ANDROID_LOG_ERROR,    tag, __VA_ARGS__)
#define LOGW(tag, ...) __android_log_print(ANDROID_LOG_WARN,     tag, __VA_ARGS__)
#define LOGI(tag, ...) __android_log_print(ANDROID_LOG_INFO,     tag, __VA_ARGS__)
#define LOGD(tag, ...) __android_log_print(ANDROID_LOG_DEBUG,    tag, __VA_ARGS__)

#endif //ANDROIDOPUSENCODER_LOGGER_H
