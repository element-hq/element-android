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

package im.vector.opusencoder.configuration

/**
 * Sampling rate of the input signal in Hz.
 */
sealed class SampleRate private constructor(val value: Int) {
    object Rate8khz : SampleRate(8000)
    object Rate12kHz : SampleRate(12000)
    object Rate16kHz : SampleRate(16000)
    object Rate24KHz : SampleRate(24000)
    object Rate48kHz : SampleRate(48000)
}
