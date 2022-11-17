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

package im.vector.app.features.voicebroadcast.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import im.vector.app.features.voice.AbstractVoiceRecorderQ
import im.vector.lib.core.utils.timer.CountUpTimer
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.Q)
class VoiceBroadcastRecorderQ(
        context: Context,
) : AbstractVoiceRecorderQ(context), VoiceBroadcastRecorder {

    private var maxFileSize = 0L // zero or negative for no limit
    private var currentRoomId: String? = null
    private var currentMaxLength: Int = 0

    override var currentSequence = 0
    override var recordingState = VoiceBroadcastRecorder.State.Idle
        set(value) {
            field = value
            listeners.forEach { it.onStateUpdated(value) }
        }
    override var currentRemainingTime: Long? = null
        set(value) {
            field = value
            listeners.forEach { it.onRemainingTimeUpdated(value) }
        }

    private val recordingTicker = RecordingTicker()
    private val listeners = CopyOnWriteArrayList<VoiceBroadcastRecorder.Listener>()

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

    override fun startRecord(roomId: String, chunkLength: Int, maxLength: Int) {
        currentRoomId = roomId
        maxFileSize = (chunkLength * audioEncodingBitRate / 8).toLong()
        currentMaxLength = maxLength
        currentSequence = 1
        startRecord(roomId)
        recordingState = VoiceBroadcastRecorder.State.Recording
        recordingTicker.start()
    }

    override fun pauseRecord() {
        tryOrNull { mediaRecorder?.stop() }
        mediaRecorder?.reset()
        recordingState = VoiceBroadcastRecorder.State.Paused
        recordingTicker.pause()
        notifyOutputFileCreated()
    }

    override fun resumeRecord() {
        currentSequence++
        currentRoomId?.let { startRecord(it) }
        recordingState = VoiceBroadcastRecorder.State.Recording
        recordingTicker.resume()
    }

    override fun stopRecord() {
        super.stopRecord()

        // Stop recording
        recordingState = VoiceBroadcastRecorder.State.Idle
        recordingTicker.stop()
        notifyOutputFileCreated()

        // Remove listeners
        listeners.clear()

        // Reset data
        currentSequence = 0
        currentMaxLength = 0
        currentRemainingTime = null
        currentRoomId = null
    }

    override fun release() {
        mediaRecorder?.setOnInfoListener(null)
        super.release()
    }

    override fun addListener(listener: VoiceBroadcastRecorder.Listener) {
        listeners.add(listener)
        listener.onStateUpdated(recordingState)
        listener.onRemainingTimeUpdated(currentRemainingTime)
    }

    override fun removeListener(listener: VoiceBroadcastRecorder.Listener) {
        listeners.remove(listener)
    }

    private fun onMaxFileSizeApproaching(roomId: String) {
        setNextOutputFile(roomId)
    }

    private fun onNextOutputFileStarted() {
        notifyOutputFileCreated()
        currentSequence++
    }

    private fun notifyOutputFileCreated() {
        outputFile?.let { file ->
            listeners.forEach { it.onVoiceMessageCreated(file, currentSequence) }
            outputFile = nextOutputFile
            nextOutputFile = null
        }
    }

    private fun onElapsedTimeUpdated(elapsedTimeMillis: Long) {
        currentRemainingTime = if (currentMaxLength > 0 && recordingState != VoiceBroadcastRecorder.State.Idle) {
            val currentMaxLengthMillis = TimeUnit.SECONDS.toMillis(currentMaxLength.toLong())
            val remainingTimeMillis = currentMaxLengthMillis - elapsedTimeMillis
            TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis)
        } else {
            null
        }
    }

    private inner class RecordingTicker(
            private var recordingTicker: CountUpTimer? = null,
    ) {
        fun start() {
            recordingTicker?.stop()
            recordingTicker = CountUpTimer().apply {
                tickListener = CountUpTimer.TickListener { onTick(elapsedTime()) }
                resume()
                onTick(elapsedTime())
            }
        }

        fun pause() {
            recordingTicker?.apply {
                pause()
                onTick(elapsedTime())
            }
        }

        fun resume() {
            recordingTicker?.apply {
                resume()
                onTick(elapsedTime())
            }
        }

        fun stop() {
            recordingTicker?.apply {
                stop()
                onTick(elapsedTime())
                recordingTicker = null
            }
        }

        private fun onTick(elapsedTimeMillis: Long) {
            onElapsedTimeUpdated(elapsedTimeMillis)
        }
    }
}
