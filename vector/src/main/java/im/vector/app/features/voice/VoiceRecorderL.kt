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

package im.vector.app.features.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import im.vector.opusencoder.OggOpusEncoder
import im.vector.opusencoder.configuration.SampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.md5
import java.io.File
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * VoiceRecorder to be used on Android versions < [Build.VERSION_CODES.Q]. It uses libopus to record ogg files.
 */
class VoiceRecorderL(
        context: Context,
        coroutineContext: CoroutineContext,
) : VoiceRecorder {

    companion object {
        private val SAMPLE_RATE = SampleRate._48kHz
        private const val BITRATE = 24 * 1024
    }

    private val outputDirectory: File by lazy { ensureAudioDirectory(context) }
    private var outputFile: File? = null

    private val recorderScope = CoroutineScope(coroutineContext)
    private var recordingJob: Job? = null

    private var audioRecorder: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private val codec = OggOpusEncoder()

    // Size of the audio buffer for Short values
    private var bufferSizeInShorts = 0
    private var maxAmplitude = 0

    private fun initializeCodec(filePath: String) {
        codec.init(filePath, SAMPLE_RATE)
        codec.setBitrate(BITRATE)

        createAudioRecord()

        val recorder = this.audioRecorder ?: return

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = tryOrNull {
                NoiseSuppressor.create(recorder.audioSessionId).also { it.enabled = true }
            }
        }

        if (AutomaticGainControl.isAvailable()) {
            automaticGainControl = tryOrNull {
                AutomaticGainControl.create(recorder.audioSessionId).also { it.enabled = true }
            }
        }
    }

    override fun initializeRecord(attachmentData: ContentAttachmentData) {
        outputFile = attachmentData.findVoiceFile(outputDirectory)
    }

    override fun startRecord(roomId: String) {
        val fileName = "${UUID.randomUUID()}.ogg"
        val outputDirectoryForRoom = File(outputDirectory, roomId.md5()).apply {
            mkdirs()
        }
        val outputFile = File(outputDirectoryForRoom, fileName)
        this.outputFile = outputFile

        initializeCodec(outputFile.absolutePath)

        recordingJob = recorderScope.launch {
            while (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
                // If the recorder is not ready let's give it some extra time
                delay(10L)
            }
            audioRecorder?.startRecording()

            val buffer = ShortArray(bufferSizeInShorts)
            while (isActive) {
                val read = audioRecorder?.read(buffer, 0, bufferSizeInShorts) ?: -1
                calculateMaxAmplitude(buffer)
                if (read <= 0) continue
                codec.encode(buffer, read)
            }
        }
    }

    override fun stopRecord() {
        recordingJob?.cancel()

        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecorder?.stop()
        }
        audioRecorder?.release()
        audioRecorder = null

        noiseSuppressor?.release()
        noiseSuppressor = null

        automaticGainControl?.release()
        automaticGainControl = null

        codec.release()
    }

    override fun cancelRecord() {
        outputFile?.delete()
        outputFile = null
    }

    override fun getMaxAmplitude(): Int {
        return maxAmplitude
    }

    override fun getVoiceMessageFile(): File? {
        return outputFile
    }

    private fun createAudioRecord() {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        bufferSizeInShorts = AudioRecord.getMinBufferSize(SAMPLE_RATE.value, channelConfig, format)
        // Buffer is created as a ShortArray, but AudioRecord needs the size in bytes
        val bufferSizeInBytes = bufferSizeInShorts * 2
        audioRecorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE.value, channelConfig, format, bufferSizeInBytes)
    }

    private fun calculateMaxAmplitude(buffer: ShortArray) {
        maxAmplitude = buffer.maxOf { it }.toInt()
    }
}
