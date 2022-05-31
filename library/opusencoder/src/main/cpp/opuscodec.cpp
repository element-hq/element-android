/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
