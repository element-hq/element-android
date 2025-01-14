/*
 * Copyright 2021-2024 New Vector Ltd.
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
 * VoiceRecorder to be used on Android versions >= [Build.VERSION_CODES.Q].
 * It uses the native OPUS support on Android 10+.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class VoiceRecorderQ(context: Context) : AbstractVoiceRecorderQ(context) {

    // We can directly use OGG here
    override val outputFormat = MediaRecorder.OutputFormat.OGG
    override val audioEncoder = MediaRecorder.AudioEncoder.OPUS

    override val fileNameExt: String = "ogg"
}
