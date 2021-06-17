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

package im.vector.app.features.home.room.detail.composer

import android.content.Context
import android.media.MediaRecorder
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import im.vector.app.BuildConfig
import im.vector.lib.multipicker.entity.MultiPickerAudioType
import im.vector.lib.multipicker.utils.toMultiPickerAudioType
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.util.UUID
import javax.inject.Inject

/**
 * Helper class to record audio for voice messages.
 */
class VoiceMessageRecordingHelper @Inject constructor(
        private val context: Context
) {

    private lateinit var mediaRecorder: MediaRecorder
    private val outputDirectory = File(context.cacheDir, "downloads")
    private var outputFile: File? = null

    init {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
    }

    private fun refreshMediaRecorder() {
        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
        mediaRecorder.setAudioEncodingBitRate(24000)
        mediaRecorder.setAudioSamplingRate(48000)
    }

    fun startRecording() {
        outputFile = File(outputDirectory, UUID.randomUUID().toString() + ".ogg")
        FileOutputStream(outputFile).use { fos ->
            refreshMediaRecorder()
            mediaRecorder.setOutputFile(fos.fd)
            mediaRecorder.prepare()
            mediaRecorder.start()
        }
    }

    fun stopRecording(recordTime: Long): MultiPickerAudioType? {
        try {
            mediaRecorder.stop()
            mediaRecorder.reset()
            mediaRecorder.release()
            outputFile?.let {
                val outputFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", it)
                return outputFileUri?.toMultiPickerAudioType(context)
            } ?: return null
        } catch (e: RuntimeException) { // Usually thrown when the record is less than 1 second.
            Timber.e(e, "Voice message is not valid. Record time: %s", recordTime)
            return null
        }
    }

    fun deleteRecording() {
        outputFile?.delete()
    }
}
