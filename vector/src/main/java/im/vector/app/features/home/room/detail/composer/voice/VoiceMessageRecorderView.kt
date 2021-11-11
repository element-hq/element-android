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

package im.vector.app.features.home.room.detail.composer.voice

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.hardware.vibrate
import im.vector.app.core.utils.CountUpTimer
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewVoiceMessageRecorderBinding
import im.vector.app.features.home.room.detail.timeline.helper.VoiceMessagePlaybackTracker
import kotlin.math.floor

/**
 * Encapsulates the voice message recording view and animations.
 */
class VoiceMessageRecorderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), VoiceMessagePlaybackTracker.Listener {

    interface Callback {
        fun onVoiceRecordingStarted()
        fun onVoiceRecordingEnded(isCancelled: Boolean)
        fun onVoiceRecordingPlaybackModeOn()
        fun onVoicePlaybackButtonClicked()
        fun onRecordingStopped()
        fun onUiStateChanged(state: RecordingUiState)
        fun sendVoiceMessage()
        fun deleteVoiceMessage()
        fun onRecordingLimitReached()
    }

    // We need to define views as lateinit var to be able to check if initialized for the bug fix on api 21 and 22.
    @Suppress("UNNECESSARY_LATEINIT")
    private lateinit var voiceMessageViews: VoiceMessageViews

    var callback: Callback? = null

    private var currentUiState: RecordingUiState = RecordingUiState.None
    private var recordingTicker: CountUpTimer? = null

    init {
        inflate(this.context, R.layout.view_voice_message_recorder, this)
        val dimensionConverter = DimensionConverter(this.context.resources)
        voiceMessageViews = VoiceMessageViews(
                this.context.resources,
                ViewVoiceMessageRecorderBinding.bind(this),
                dimensionConverter
        )
        initVoiceRecordingViews()
        initListeners()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // onVisibilityChanged is called by constructor on api 21 and 22.
        if (!this::voiceMessageViews.isInitialized) return
        val parentChanged = changedView == this
        voiceMessageViews.renderVisibilityChanged(parentChanged, visibility)
    }

    fun initVoiceRecordingViews() {
        stopRecordingTicker()
        voiceMessageViews.initViews(onVoiceRecordingEnded = {})
    }

    private fun initListeners() {
        voiceMessageViews.start(object : VoiceMessageViews.Actions {
            override fun onRequestRecording() {
                callback?.onVoiceRecordingStarted()
            }

            override fun onRecordingStopped() {
                callback?.onRecordingStopped()
            }

            override fun isActive() = currentUiState != RecordingUiState.Cancelled

            override fun updateState(updater: (RecordingUiState) -> RecordingUiState) {
                updater(currentUiState).also { newState ->
                    when (newState) {
                        is DraggingState -> display(newState)
                        else             -> {
                            if (newState != currentUiState) {
                                callback?.onUiStateChanged(newState)
                            }
                        }
                    }
                }
            }

            override fun sendMessage() {
                callback?.sendVoiceMessage()
            }

            override fun delete() {
                // this was previously marked as cancelled true
                callback?.deleteVoiceMessage()
            }

            override fun waveformClicked() {
                display(RecordingUiState.Playback)
            }

            override fun onVoicePlaybackButtonClicked() {
                callback?.onVoicePlaybackButtonClicked()
            }
        })
    }

    fun display(recordingState: RecordingUiState) {
        if (recordingState == this.currentUiState) return

        val previousState = this.currentUiState
        this.currentUiState = recordingState
        when (recordingState) {
            RecordingUiState.None      -> {
                val isCancelled = previousState == RecordingUiState.Cancelled
                voiceMessageViews.hideRecordingViews(recordingState, isCancelled = isCancelled) { callback?.onVoiceRecordingEnded(it) }
                stopRecordingTicker()
            }
            RecordingUiState.Started   -> {
                startRecordingTicker()
                voiceMessageViews.renderToast(context.getString(R.string.voice_message_release_to_send_toast))
                voiceMessageViews.showRecordingViews()
            }
            RecordingUiState.Cancelled -> {
                voiceMessageViews.hideRecordingViews(recordingState, isCancelled = true) { callback?.onVoiceRecordingEnded(it) }
                vibrate(context)
            }
            RecordingUiState.Locked    -> {
                voiceMessageViews.renderLocked()
                postDelayed({
                    voiceMessageViews.showRecordingLockedViews(recordingState) { callback?.onVoiceRecordingEnded(it) }
                }, 500)
            }
            RecordingUiState.Playback  -> {
                stopRecordingTicker()
                voiceMessageViews.showPlaybackViews()
                callback?.onVoiceRecordingPlaybackModeOn()
            }
            is DraggingState           -> when (recordingState) {
                is DraggingState.Cancelling -> voiceMessageViews.renderCancelling(recordingState.distanceX)
                is DraggingState.Locking    -> voiceMessageViews.renderLocking(recordingState.distanceY)
            }.exhaustive
        }
    }

    private fun startRecordingTicker() {
        recordingTicker?.stop()
        recordingTicker = CountUpTimer().apply {
            tickListener = object : CountUpTimer.TickListener {
                override fun onTick(milliseconds: Long) {
                    onRecordingTick(milliseconds)
                }
            }
            resume()
        }
        onRecordingTick(0L)
    }

    private fun onRecordingTick(milliseconds: Long) {
        voiceMessageViews.renderRecordingTimer(currentUiState, milliseconds / 1_000)
        val timeDiffToRecordingLimit = BuildConfig.VOICE_MESSAGE_DURATION_LIMIT_MS - milliseconds
        if (timeDiffToRecordingLimit <= 0) {
            post {
                callback?.onRecordingLimitReached()
            }
        } else if (timeDiffToRecordingLimit in 10_000..10_999) {
            post {
                voiceMessageViews.renderToast(context.getString(R.string.voice_message_n_seconds_warning_toast, floor(timeDiffToRecordingLimit / 1000f).toInt()))
                vibrate(context)
            }
        }
    }

    private fun stopRecordingTicker() {
        recordingTicker?.stop()
        recordingTicker = null
    }

    /**
     * Returns true if the voice message is recording or is in playback mode
     */
    fun isActive() = currentUiState !in listOf(RecordingUiState.None, RecordingUiState.Cancelled)

    override fun onUpdate(state: VoiceMessagePlaybackTracker.Listener.State) {
        when (state) {
            is VoiceMessagePlaybackTracker.Listener.State.Recording -> {
                voiceMessageViews.renderRecordingWaveform(state.amplitudeList.toTypedArray())
            }
            is VoiceMessagePlaybackTracker.Listener.State.Playing   -> {
                voiceMessageViews.renderPlaying(state)
            }
            is VoiceMessagePlaybackTracker.Listener.State.Paused,
            is VoiceMessagePlaybackTracker.Listener.State.Idle      -> {
                voiceMessageViews.renderIdle()
            }
        }
    }

    sealed interface RecordingUiState {
        object None : RecordingUiState
        object Started : RecordingUiState
        object Cancelled : RecordingUiState
        object Locked : RecordingUiState
        object Playback : RecordingUiState
    }

    sealed interface DraggingState : RecordingUiState {
        data class Cancelling(val distanceX: Float) : DraggingState
        data class Locking(val distanceY: Float) : DraggingState
    }
}
