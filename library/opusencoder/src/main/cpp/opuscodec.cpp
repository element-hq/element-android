#include <jni.h>
#include "codec/CodecOggOpus.h"

CodecOggOpus oggCodec;

extern "C"
JNIEXPORT jint JNICALL Java_im_vector_opusencoder_OggOpusEncoder_init(JNIEnv *env, jobject thiz, jstring file_path, jint sample_rate) {
    char *path = (char*) env->GetStringUTFChars(file_path, 0);
    return oggCodec.encoderInit(path, sample_rate);
}

extern "C"
JNIEXPORT jint JNICALL Java_im_vector_opusencoder_OggOpusEncoder_writeFrame(JNIEnv *env, jobject thiz, jshortArray shorts, jint samples_per_channel) {
    jshort *nativeShorts = env->GetShortArrayElements(shorts, 0);
    return oggCodec.writeFrame((short *) nativeShorts, samples_per_channel);
}

extern "C"
JNIEXPORT jint JNICALL Java_im_vector_opusencoder_OggOpusEncoder_setBitrate(JNIEnv *env, jobject thiz, jint bitrate) {
    return oggCodec.setBitrate(bitrate);
}

extern "C"
JNIEXPORT void JNICALL Java_im_vector_opusencoder_OggOpusEncoder_encoderRelease(JNIEnv *env, jobject thiz) {
    oggCodec.encoderRelease();
}
