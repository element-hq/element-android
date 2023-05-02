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
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File

/**
 * VoiceRecorder abstraction to be used on Android versions >= [Build.VERSION_CODES.Q].
 */
@RequiresApi(Build.VERSION_CODES.Q)
abstract class AbstractVoiceRecorderQ(protected val context: Context) : AbstractVoiceRecorder(context) {

    var mediaRecorder: MediaRecorder? = null
    protected var nextOutputFile: File? = null

    private val audioSource: Int = MediaRecorder.AudioSource.DEFAULT
    private val audioSamplingRate: Int = 48_000
    protected val audioEncodingBitRate: Int = 24_000

    abstract val outputFormat: Int // see MediaRecorder.OutputFormat
    abstract val audioEncoder: Int // see MediaRecorder.AudioEncoder

    override fun initializeRecord(roomId: String, attachmentData: ContentAttachmentData?) {
        super.initializeRecord(roomId, attachmentData)
        mediaRecorder = createMediaRecorder().apply {
            setAudioSource(audioSource)
            setOutputFormat()
            setAudioEncodingBitRate(audioEncodingBitRate)
            setAudioSamplingRate(audioSamplingRate)
        }
        setOutputFile(roomId)
    }

    override fun startRecord(roomId: String) {
        initializeRecord(roomId = roomId)
        mediaRecorder?.prepare()
        mediaRecorder?.start()
    }

    override fun pauseRecord() {
        // Can throw when the record is less than 1 second.
        tryOrNull { mediaRecorder?.pause() }
    }

    override fun resumeRecord() {
        mediaRecorder?.resume()
    }

    override fun stopRecord() {
        // Can throw when the record is less than 1 second.
        tryOrNull { mediaRecorder?.stop() }
        mediaRecorder?.reset()
        release()
    }

    override fun cancelRecord() {
        super.cancelRecord()
        nextOutputFile?.delete()
        nextOutputFile = null
    }

    override fun getMaxAmplitude(): Int {
        return mediaRecorder?.maxAmplitude ?: 0
    }

    protected open fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    fun setNextOutputFile(roomId: String) {
        val mediaRecorder = mediaRecorder ?: return
        nextOutputFile = createOutputFile(roomId)
        mediaRecorder.setNextOutputFile(nextOutputFile)
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun MediaRecorder.setOutputFormat() {
        setOutputFormat(outputFormat)
        setAudioEncoder(audioEncoder)
    }

    private fun setOutputFile(roomId: String) {
        val mediaRecorder = mediaRecorder ?: return
        outputFile = outputFile ?: createOutputFile(roomId)
        mediaRecorder.setOutputFile(outputFile)
    }
}
