/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.VisibleForTesting
import im.vector.app.features.VectorFeatures
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class VoiceRecorderProvider @Inject constructor(
        private val context: Context,
        private val vectorFeatures: VectorFeatures,
) {
    fun provideVoiceRecorder(): VoiceRecorder {
        return if (useFallbackRecorder()) {
            VoiceRecorderL(context, Dispatchers.IO)
        } else {
            VoiceRecorderQ(context)
        }
    }

    private fun useFallbackRecorder(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !hasOpusEncoder() || vectorFeatures.forceUsageOfOpusEncoder()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun hasOpusEncoder(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48000, 1)
        return codecList.findEncoderForFormat(format) != null
    }
}
