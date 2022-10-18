/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.voice

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import im.vector.app.features.VectorFeatures
import io.element.android.opusencoder.OggOpusEncoder
import kotlinx.coroutines.Dispatchers
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import javax.inject.Inject

class VoiceRecorderProvider @Inject constructor(
        private val context: Context,
        private val vectorFeatures: VectorFeatures,
        private val buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
) {
    fun provideVoiceRecorder(): VoiceRecorder {
        return if (useNativeRecorder()) {
            VoiceRecorderQ(context)
        } else {
            VoiceRecorderL(context, Dispatchers.IO, OggOpusEncoder.create())
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    private fun useNativeRecorder(): Boolean {
        return buildVersionSdkIntProvider.get() >= Build.VERSION_CODES.Q &&
                hasOpusEncoder() &&
                !vectorFeatures.forceUsageOfOpusEncoder()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun hasOpusEncoder(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48000, 1)
        return codecList.findEncoderForFormat(format) != null
    }
}
