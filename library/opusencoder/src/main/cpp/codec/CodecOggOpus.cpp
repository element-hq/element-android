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

#include "CodecOggOpus.h"
#include "../utils/Logger.h"

int ret;

int CodecOggOpus::encoderInit(char* filePath, int sampleRate) {
    // Create default, empty comment header
    comments = ope_comments_create();
    // Mono audio
    int numChannels = 1;
    // Channel Mapping Family 0, used for mono/stereo streams
    int family = 0;
    // Create encoder to encode PCM chunks and write the result to a file with the OggOpus framing
    encoder = ope_encoder_create_file(filePath, comments, sampleRate, numChannels, family, &ret);
    if (ret != OPE_OK) {
        LOGE(TAG, "Creation of OggOpusEnc failed.");
        return ret;
    }
    return OPE_OK;
}

int CodecOggOpus::setBitrate(int bitrate) {
    ret = ope_encoder_ctl(encoder, OPUS_SET_BITRATE_REQUEST, bitrate);
    if (ret != OPE_OK) {
        LOGE(TAG, "Could not set bitrate.");
        return ret;
    }
    return OPE_OK;
}

int CodecOggOpus::writeFrame(short* frame, int samplesPerChannel) {
    // Encode the raw PCM-16 buffer to Opus and write it to the ogg file
    return ope_encoder_write(encoder, frame, samplesPerChannel);
}

void CodecOggOpus::encoderRelease() {
    // Finish any pending encode/write operations
    ope_encoder_drain(encoder);
    // De-init the encoder instance
    ope_encoder_destroy(encoder);
    // De-init the comment header struct
    ope_comments_destroy(comments);
}

CodecOggOpus::~CodecOggOpus() {
    encoderRelease();
}
