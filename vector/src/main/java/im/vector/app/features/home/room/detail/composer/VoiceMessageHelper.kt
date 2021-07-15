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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.FileProvider
import im.vector.app.BuildConfig
import im.vector.app.core.utils.CountUpTimer
import im.vector.app.features.home.room.detail.timeline.helper.VoiceMessagePlaybackTracker
import im.vector.app.features.voice.VoiceFailure
import im.vector.lib.multipicker.entity.MultiPickerAudioType
import im.vector.lib.multipicker.utils.toMultiPickerAudioType
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Helper class to record audio for voice messages.
 */
class VoiceMessageHelper @Inject constructor(
        private val context: Context,
        private val playbackTracker: VoiceMessagePlaybackTracker
) {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private val outputDirectory = File(context.cacheDir, "downloads")
    private var outputFile: File? = null
    private var lastRecordingFile: File? = null // In case of user pauses recording, plays another one in timeline

    private val amplitudeList = mutableListOf<Int>()

    private var amplitudeTicker: CountUpTimer? = null
    private var playbackTicker: CountUpTimer? = null

    init {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
    }

    private fun initMediaRecorder() {
        MediaRecorder().let {
            it.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            it.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            it.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            it.setAudioEncodingBitRate(24000)
            it.setAudioSamplingRate(48000)
            mediaRecorder = it
        }
    }

    fun startRecording() {
        stopPlayback()
        playbackTracker.makeAllPlaybacksIdle()

        outputFile = File(outputDirectory, "Voice message.ogg")
        lastRecordingFile = outputFile
        amplitudeList.clear()

        try {
            initMediaRecorder()
            val mr = mediaRecorder!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mr.setOutputFile(outputFile)
            } else {
                mr.setOutputFile(FileOutputStream(outputFile).fd)
            }
            mr.prepare()
            mr.start()
        } catch (failure: Throwable) {
            throw VoiceFailure.UnableToRecord(failure)
        }
        startRecordingAmplitudes()
    }

    fun stopRecording(): MultiPickerAudioType? {
        internalStopRecording()
        try {
            outputFile?.let {
                val outputFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", it)
                return outputFileUri
                        ?.toMultiPickerAudioType(context)
                        ?.apply {
                            waveform = amplitudeList
                        }
            } ?: return null
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Cannot stop voice recording")
            return null
        }
    }

    private fun internalStopRecording() {
        tryOrNull("Cannot stop media recording amplitude") {
            stopRecordingAmplitudes()
        }
        tryOrNull("Cannot stop media recorder!") {
            // Usually throws when the record is less than 1 second.
            releaseMediaRecorder()
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.let {
            it.stop()
            it.reset()
            it.release()
        }

        mediaRecorder = null
    }

    fun pauseRecording() {
        releaseMediaRecorder()
    }

    fun deleteRecording() {
        internalStopRecording()
        outputFile?.delete()
        outputFile = null
    }

    fun startOrPauseRecordingPlayback() {
        lastRecordingFile?.let {
            startOrPausePlayback(VoiceMessagePlaybackTracker.RECORDING_ID, it)
        }
    }

    fun startOrPausePlayback(id: String, file: File) {
        stopPlayback()
        if (playbackTracker.getPlaybackState(id) is VoiceMessagePlaybackTracker.Listener.State.Playing) {
            playbackTracker.pausePlayback(id)
        } else {
            startPlayback(id, file)
            playbackTracker.startPlayback(id)
        }
    }

    private fun startPlayback(id: String, file: File) {
        val currentPlaybackTime = playbackTracker.getPlaybackTime(id)

        try {
            FileInputStream(file).use { fis ->
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                            AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                    )
                    setDataSource(fis.fd)
                    prepare()
                    start()
                    seekTo(currentPlaybackTime)
                }
            }
        } catch (failure: Throwable) {
            throw VoiceFailure.UnableToPlay(failure)
        }
        startPlaybackTicker(id)
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        stopPlaybackTicker()
    }

    private fun startRecordingAmplitudes() {
        amplitudeTicker?.stop()
        amplitudeTicker = CountUpTimer(100).apply {
            tickListener = object : CountUpTimer.TickListener {
                override fun onTick(milliseconds: Long) {
                    onAmplitudeTick()
                }
            }
            resume()
        }
    }

    private fun onAmplitudeTick() {
        val mr = mediaRecorder ?: return
        try {
            val maxAmplitude = mr.maxAmplitude
            amplitudeList.add(maxAmplitude)
            playbackTracker.updateCurrentRecording(VoiceMessagePlaybackTracker.RECORDING_ID, amplitudeList)
        } catch (e: IllegalStateException) {
            Timber.e(e, "Cannot get max amplitude. Amplitude recording timer will be stopped.")
            stopRecordingAmplitudes()
        } catch (e: RuntimeException) {
            Timber.e(e, "Cannot get max amplitude (native error). Amplitude recording timer will be stopped.")
            stopRecordingAmplitudes()
        }
    }

    private fun stopRecordingAmplitudes() {
        amplitudeTicker?.stop()
        amplitudeTicker = null
    }

    private fun startPlaybackTicker(id: String) {
        playbackTicker?.stop()
        playbackTicker = CountUpTimer().apply {
            tickListener = object : CountUpTimer.TickListener {
                override fun onTick(milliseconds: Long) {
                    onPlaybackTick(id)
                }
            }
            resume()
        }
        onPlaybackTick(id)
    }

    private fun onPlaybackTick(id: String) {
        if (mediaPlayer?.isPlaying.orFalse()) {
            val currentPosition = mediaPlayer?.currentPosition ?: 0
            playbackTracker.updateCurrentPlaybackTime(id, currentPosition)
        } else {
            playbackTracker.stopPlayback(id)
            stopPlaybackTicker()
        }
    }

    private fun stopPlaybackTicker() {
        playbackTicker?.stop()
        playbackTicker = null
    }

    fun stopAllVoiceActions() {
        stopRecording()
        stopPlayback()
        deleteRecording()
        playbackTracker.clear()
    }
}
