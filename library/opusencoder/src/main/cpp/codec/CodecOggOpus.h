//
// Created by Jorge Martín Espinosa on 30/5/22.
//

#ifndef ELEMENT_ANDROID_CODECOGGOPUS_H
#define ELEMENT_ANDROID_CODECOGGOPUS_H

#include <opusenc.h>

class CodecOggOpus {

private:
    const char *TAG = "CodecOggOpus";

    OggOpusEnc* encoder;
    OggOpusComments* comments;

public:
    int encoderInit(char* filePath, int sampleRate);

    int setBitrate(int bitrate);

    int writeFrame(short *frame, int samplesPerChannel);

    void encoderRelease();

    ~CodecOggOpus();
};


#endif //ELEMENT_ANDROID_CODECOGGOPUS_H
