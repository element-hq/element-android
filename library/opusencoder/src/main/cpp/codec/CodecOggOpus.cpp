//
// Created by Jorge Martín Espinosa on 30/5/22.
//

#include "CodecOggOpus.h"
#include "../utils/Logger.h"

int ret;

int CodecOggOpus::encoderInit(char* filePath, int sampleRate) {
    comments = ope_comments_create();
    int numChannels = 1; // Mono audio
    int family = 0; // Channel Mapping Family 0, used for mono/stereo streams
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
    return ope_encoder_write(encoder, frame, samplesPerChannel);
}

void CodecOggOpus::encoderRelease() {
    ope_encoder_drain(encoder);
    ope_encoder_destroy(encoder);
    ope_comments_destroy(comments);
}

CodecOggOpus::~CodecOggOpus() {
    encoderRelease();
}
