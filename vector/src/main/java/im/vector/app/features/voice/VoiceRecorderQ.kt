/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * VoiceRecorder to be used on Android versions >= [Build.VERSION_CODES.Q]. It uses the native OPUS support on Android 10+.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class VoiceRecorderQ(context: Context) : AbstractVoiceRecorder(context, "ogg") {
    override fun setOutputFormat(mediaRecorder: MediaRecorder) {
        // We can directly use OGG here
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
    }
}
