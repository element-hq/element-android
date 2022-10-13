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

package im.vector.app.features.voicebroadcast

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import im.vector.app.features.voice.AbstractVoiceRecorderQ
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
class VoiceBroadcastRecorder(
        context: Context,
) : AbstractVoiceRecorderQ(context) {

    private val maxFileSize = 25_000L // 0,025 Mb = 25 Kb ~= 6s

    var listener: Listener? = null

    override val outputFormat = MediaRecorder.OutputFormat.MPEG_4
    override val audioEncoder = MediaRecorder.AudioEncoder.HE_AAC

    override val fileNameExt: String = "mp4"

    override fun initializeRecord(roomId: String, attachmentData: ContentAttachmentData?) {
        super.initializeRecord(roomId, attachmentData)
        mediaRecorder?.setMaxFileSize(maxFileSize)
        mediaRecorder?.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> onMaxFileSizeApproaching(roomId)
                MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED -> onNextOutputFileStarted()
                else -> Unit // Nothing to do
            }
        }
    }

    override fun stopRecord() {
        super.stopRecord()
        notifyOutputFileCreated()
        listener = null
    }

    override fun release() {
        mediaRecorder?.setOnInfoListener(null)
        super.release()
    }

    private fun onMaxFileSizeApproaching(roomId: String) {
        setNextOutputFile(roomId)
    }

    private fun onNextOutputFileStarted() {
        notifyOutputFileCreated()
    }

    private fun notifyOutputFileCreated() {
        outputFile?.let {
            listener?.onVoiceMessageCreated(it)
            outputFile = nextOutputFile
            nextOutputFile = null
        }
    }

    fun interface Listener {
        fun onVoiceMessageCreated(file: File)
    }
}
