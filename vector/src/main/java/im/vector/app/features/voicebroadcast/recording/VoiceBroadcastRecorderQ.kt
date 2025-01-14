/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.recording

import android.content.Context
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.voice.AbstractVoiceRecorderQ
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastStateEventLiveUseCase
import im.vector.lib.core.utils.timer.CountUpTimer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.flow.flow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.Q)
class VoiceBroadcastRecorderQ(
        context: Context,
        private val sessionHolder: ActiveSessionHolder,
        private val getVoiceBroadcastEventUseCase: GetVoiceBroadcastStateEventLiveUseCase
) : AbstractVoiceRecorderQ(context), VoiceBroadcastRecorder {

    private val session get() = sessionHolder.getActiveSession()
    private val sessionScope get() = session.coroutineScope

    private var voiceBroadcastStateObserver: Job? = null
    private var syncStateObserver: Job? = null

    private var maxFileSize = 0L // zero or negative for no limit
    private var currentVoiceBroadcast: VoiceBroadcast? = null
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

    override fun startRecordVoiceBroadcast(voiceBroadcast: VoiceBroadcast, chunkLength: Int, maxLength: Int) {
        // Stop recording previous voice broadcast if any
        if (recordingState != VoiceBroadcastRecorder.State.Idle) stopRecord()

        currentVoiceBroadcast = voiceBroadcast
        maxFileSize = (chunkLength * audioEncodingBitRate / 8).toLong()
        currentMaxLength = maxLength
        currentSequence = 1

        observeVoiceBroadcastStateEvent(voiceBroadcast)
    }

    override fun startRecord(roomId: String) {
        super.startRecord(roomId)
        observeConnectionState()
    }

    override fun pauseOnError() {
        if (recordingState != VoiceBroadcastRecorder.State.Recording) return

        pauseRecorder()
        stopObservingConnectionState()
        recordingState = VoiceBroadcastRecorder.State.Error
        notifyError()
    }

    override fun pauseRecord() {
        if (recordingState !in arrayOf(VoiceBroadcastRecorder.State.Recording, VoiceBroadcastRecorder.State.Error)) return

        pauseRecorder()
        stopObservingConnectionState()
        recordingState = VoiceBroadcastRecorder.State.Paused
        notifyOutputFileCreated()
    }

    override fun resumeRecord() {
        if (recordingState != VoiceBroadcastRecorder.State.Paused) return

        currentSequence++
        currentVoiceBroadcast?.let { startRecord(it.roomId) }
        recordingState = VoiceBroadcastRecorder.State.Recording
        recordingTicker.resume()
        observeConnectionState()
    }

    override fun stopRecord() {
        super.stopRecord()

        // Stop recording
        recordingState = VoiceBroadcastRecorder.State.Idle
        recordingTicker.stop()
        notifyOutputFileCreated()

        // Remove listeners
        listeners.clear()

        // Do not observe anymore voice broadcast changes
        voiceBroadcastStateObserver?.cancel()
        voiceBroadcastStateObserver = null

        stopObservingConnectionState()

        // Reset data
        currentSequence = 0
        currentMaxLength = 0
        currentRemainingTime = null
        currentVoiceBroadcast = null
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

    private fun observeVoiceBroadcastStateEvent(voiceBroadcast: VoiceBroadcast) {
        voiceBroadcastStateObserver = getVoiceBroadcastEventUseCase.execute(voiceBroadcast)
                .onEach { onVoiceBroadcastStateEventUpdated(voiceBroadcast, it.getOrNull()) }
                .launchIn(sessionScope)
    }

    private fun onVoiceBroadcastStateEventUpdated(voiceBroadcast: VoiceBroadcast, event: VoiceBroadcastEvent?) {
        when (event?.content?.voiceBroadcastState) {
            VoiceBroadcastState.STARTED -> {
                startRecord(voiceBroadcast.roomId)
                recordingState = VoiceBroadcastRecorder.State.Recording
                recordingTicker.start()
            }
            VoiceBroadcastState.PAUSED -> pauseRecord()
            VoiceBroadcastState.RESUMED -> resumeRecord()
            VoiceBroadcastState.STOPPED,
            null -> stopRecord()
        }
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

    private fun notifyError() {
        val ringtoneUri = Uri.parse("android.resource://${context.packageName}/${R.raw.vberror}")
        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone?.play()
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

    private fun pauseRecorder() {
        if (recordingState != VoiceBroadcastRecorder.State.Recording) return

        tryOrNull { mediaRecorder?.stop() }
        mediaRecorder?.reset()
        recordingTicker.pause()
    }

    private fun observeConnectionState() {
        syncStateObserver = session.flow().liveSyncState()
                .distinctUntilChanged()
                .filter { it is SyncState.NoNetwork }
                .onEach { pauseOnError() }
                .launchIn(sessionScope)
    }

    private fun stopObservingConnectionState() {
        syncStateObserver?.cancel()
        syncStateObserver = null
    }

    private inner class RecordingTicker(
            private var recordingTicker: CountUpTimer? = null,
    ) {
        fun start() {
            recordingTicker?.stop()
            recordingTicker = CountUpTimer().also {
                it.tickListener = CountUpTimer.TickListener { tick -> onTick(tick) }
                it.start()
            }
        }

        fun pause() {
            recordingTicker?.pause()
        }

        fun resume() {
            recordingTicker?.resume()
        }

        fun stop() {
            recordingTicker?.stop()
            recordingTicker = null
        }

        private fun onTick(elapsedTimeMillis: Long) {
            onElapsedTimeUpdated(elapsedTimeMillis)
        }
    }
}
