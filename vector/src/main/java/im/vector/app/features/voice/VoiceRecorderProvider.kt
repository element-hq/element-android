/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
