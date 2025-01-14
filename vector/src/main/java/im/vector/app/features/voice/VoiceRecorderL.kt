/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.widget.Toast
import io.element.android.opusencoder.OggOpusEncoder
import io.element.android.opusencoder.configuration.SampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import kotlin.coroutines.CoroutineContext

/**
 * VoiceRecorder to be used on Android versions < [Build.VERSION_CODES.Q]. It uses libopus to record ogg files.
 */
class VoiceRecorderL(
        private val context: Context,
        coroutineContext: CoroutineContext,
        private val codec: OggOpusEncoder,
) : AbstractVoiceRecorder(context) {

    companion object {
        private val SAMPLE_RATE = SampleRate.Rate48kHz
        private const val BITRATE = 24 * 1024
    }

    private val recorderScope = CoroutineScope(coroutineContext)
    private var recordingJob: Job? = null

    private var audioRecorder: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    // Size of the audio buffer for Short values
    private var bufferSizeInShorts = 0
    private var maxAmplitude = 0

    override val fileNameExt: String = "ogg"

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

    override fun startRecord(roomId: String) {
        outputFile = createOutputFile(roomId).also {
            initializeCodec(it.absolutePath)
        }

        recordingJob = recorderScope.launch {
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

    override fun pauseRecord() {
        Toast.makeText(context, "Not implemented for this Android version", Toast.LENGTH_SHORT).show()
    }

    override fun resumeRecord() {
        Toast.makeText(context, "Not implemented for this Android version", Toast.LENGTH_SHORT).show()
    }

    override fun stopRecord() {
        val recorder = this.audioRecorder ?: return
        recordingJob?.cancel()

        if (recorder.state == AudioRecord.STATE_INITIALIZED) {
            recorder.stop()
        }
        recorder.release()
        audioRecorder = null

        noiseSuppressor?.release()
        noiseSuppressor = null

        automaticGainControl?.release()
        automaticGainControl = null

        codec.release()
    }

    override fun getMaxAmplitude(): Int {
        return maxAmplitude
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
