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

#ifndef ELEMENT_ANDROID_CODECOGGOPUS_H
#define ELEMENT_ANDROID_CODECOGGOPUS_H

#include <opusenc.h>
/**
 * This class is a wrapper around libopusenc, used to encode and write Opus frames into an Ogg file.
 *
 * The usual flow would be:
 *
 * 1. Use encoderInit to initialize the internal encoder with the sample rate and the path to write the encoded frames to.
 * 2. (Optional) set the bitrate to use.
 * 3. While recording, read PCM-16 chunks from the recorder, feed them to the encoder using writeFrame.
 * 4. When finished, call encoderRelease to free some resources.
 */
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
