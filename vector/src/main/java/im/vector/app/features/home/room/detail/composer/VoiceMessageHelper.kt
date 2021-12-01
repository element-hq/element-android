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
import androidx.core.content.FileProvider
import im.vector.app.BuildConfig
import im.vector.app.core.utils.CountUpTimer
import im.vector.app.features.home.room.detail.timeline.helper.VoiceMessagePlaybackTracker
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voice.VoiceRecorder
import im.vector.app.features.voice.VoiceRecorderProvider
import im.vector.lib.multipicker.entity.MultiPickerAudioType
import im.vector.lib.multipicker.utils.toMultiPickerAudioType
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * Helper class to record audio for voice messages.
 */
class VoiceMessageHelper @Inject constructor(
        private val context: Context,
        private val playbackTracker: VoiceMessagePlaybackTracker,
        voiceRecorderProvider: VoiceRecorderProvider
) {
    private var mediaPlayer: MediaPlayer? = null
    private var voiceRecorder: VoiceRecorder = voiceRecorderProvider.provideVoiceRecorder()

    private val amplitudeList = mutableListOf<Int>()

    private var amplitudeTicker: CountUpTimer? = null
    private var playbackTicker: CountUpTimer? = null

    fun initializeRecorder(attachmentData: ContentAttachmentData) {
        voiceRecorder.initializeRecord(attachmentData)
        amplitudeList.clear()
        attachmentData.waveform?.let {
            amplitudeList.addAll(it)
            playbackTracker.updateCurrentRecording(VoiceMessagePlaybackTracker.RECORDING_ID, amplitudeList)
        }
    }

    fun startRecording(roomId: String) {
        stopPlayback()
        playbackTracker.makeAllPlaybacksIdle()
        amplitudeList.clear()

        try {
            voiceRecorder.startRecord(roomId)
        } catch (failure: Throwable) {
            Timber.e(failure, "Unable to start recording")
            throw VoiceFailure.UnableToRecord(failure)
        }
        startRecordingAmplitudes()
    }

    fun stopRecording(convertForSending: Boolean): MultiPickerAudioType? {
        tryOrNull("Cannot stop media recording amplitude") {
            stopRecordingAmplitudes()
        }
        val voiceMessageFile = tryOrNull("Cannot stop media recorder!") {
            voiceRecorder.stopRecord()
            if (convertForSending) {
                voiceRecorder.getVoiceMessageFile()
            } else {
                voiceRecorder.getCurrentRecord()
            }
        }

        try {
            voiceMessageFile?.let {
                val outputFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", it, "Voice message.${it.extension}")
                return outputFileUri
                        .toMultiPickerAudioType(context)
                        ?.apply {
                            waveform = if (amplitudeList.size < 50) {
                                amplitudeList
                            } else {
                                amplitudeList.chunked(amplitudeList.size / 50) { items -> items.maxOrNull() ?: 0 }
                            }
                        }
            } ?: return null
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Cannot stop voice recording")
            return null
        }
    }

    /**
     * When entering in playback mode actually
     */
    fun pauseRecording() {
        voiceRecorder.stopRecord()
        stopRecordingAmplitudes()
    }

    fun deleteRecording() {
        tryOrNull("Cannot stop media recording amplitude") {
            stopRecordingAmplitudes()
        }
        tryOrNull("Cannot stop media recorder!") {
            voiceRecorder.cancelRecord()
        }
    }

    fun startOrPauseRecordingPlayback() {
        voiceRecorder.getCurrentRecord()?.let {
            startOrPausePlayback(VoiceMessagePlaybackTracker.RECORDING_ID, it)
        }
    }

    fun startOrPausePlayback(id: String, file: File) {
        stopPlayback()
        stopRecordingAmplitudes()
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
                                    // Do not use CONTENT_TYPE_SPEECH / USAGE_VOICE_COMMUNICATION because we want to play loud here
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
            Timber.e(failure, "Unable to start playback")
            throw VoiceFailure.UnableToPlay(failure)
        }
        startPlaybackTicker(id)
    }

    fun stopPlayback() {
        playbackTracker.stopPlayback(VoiceMessagePlaybackTracker.RECORDING_ID)
        mediaPlayer?.stop()
        stopPlaybackTicker()
    }

    private fun startRecordingAmplitudes() {
        amplitudeTicker?.stop()
        amplitudeTicker = CountUpTimer(50).apply {
            tickListener = object : CountUpTimer.TickListener {
                override fun onTick(milliseconds: Long) {
                    onAmplitudeTick()
                }
            }
            resume()
        }
    }

    private fun onAmplitudeTick() {
        try {
            val maxAmplitude = voiceRecorder.getMaxAmplitude()
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

    fun clearTracker() {
        playbackTracker.clear()
    }

    fun stopAllVoiceActions(deleteRecord: Boolean = true): MultiPickerAudioType? {
        val audioType = stopRecording(convertForSending = false)
        stopPlayback()
        if (deleteRecord) {
            deleteRecording()
        }
        return audioType
    }
}
