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
        fun onVoiceRecordingEnded()
        fun onVoicePlaybackButtonClicked()
        fun onVoiceRecordingCancelled()
        fun onUiStateChanged(state: RecordingUiState)
        fun onSendVoiceMessage()
        fun onDeleteVoiceMessage()
        fun onRecordingLimitReached()
        fun onRecordingWaveformClicked()
    }

    // We need to define views as lateinit var to be able to check if initialized for the bug fix on api 21 and 22.
    @Suppress("UNNECESSARY_LATEINIT")
    private lateinit var voiceMessageViews: VoiceMessageViews
    lateinit var callback: Callback

    private var recordingTicker: CountUpTimer? = null
    private var lastKnownState: RecordingUiState? = null

    init {
        inflate(this.context, R.layout.view_voice_message_recorder, this)
        val dimensionConverter = DimensionConverter(this.context.resources)
        voiceMessageViews = VoiceMessageViews(
                this.context.resources,
                ViewVoiceMessageRecorderBinding.bind(this),
                dimensionConverter
        )
        initListeners()
    }

    private fun initListeners() {
        voiceMessageViews.start(object : VoiceMessageViews.Actions {
            override fun onRequestRecording() = callback.onVoiceRecordingStarted()
            override fun onMicButtonReleased() {
                when (lastKnownState) {
                    RecordingUiState.Locked    -> {
                        // do nothing,
                        // onSendVoiceMessage, onDeleteVoiceMessage or onRecordingLimitReached will be triggered instead
                    }
                    RecordingUiState.Cancelled -> callback.onVoiceRecordingCancelled()
                    else                       -> callback.onVoiceRecordingEnded()
                }
            }

            override fun onSendVoiceMessage() = callback.onSendVoiceMessage()
            override fun onDeleteVoiceMessage() = callback.onDeleteVoiceMessage()
            override fun onWaveformClicked() = callback.onRecordingWaveformClicked()
            override fun onVoicePlaybackButtonClicked() = callback.onVoicePlaybackButtonClicked()
            override fun onMicButtonDrag(updater: (RecordingUiState) -> RecordingUiState) {
                when (val currentState = lastKnownState) {
                    null, RecordingUiState.None -> {
                        // ignore drag events when the view is idle
                    }
                    else                        -> {
                        updater(currentState).also { newState ->
                            when (newState) {
                                // display drag events directly without leaving the view for faster UI feedback
                                is DraggingState -> display(newState)
                                else             -> callback.onUiStateChanged(newState)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // onVisibilityChanged is called by constructor on api 21 and 22.
        if (!this::voiceMessageViews.isInitialized) return
        val parentChanged = changedView == this
        voiceMessageViews.renderVisibilityChanged(parentChanged, visibility)
    }

    fun display(recordingState: RecordingUiState) {
        if (lastKnownState == recordingState) return
        val previousState = lastKnownState
        lastKnownState = recordingState
        when (recordingState) {
            RecordingUiState.None      -> {
                stopRecordingTicker()
                voiceMessageViews.initViews()
            }
            RecordingUiState.Started   -> {
                startRecordingTicker()
                voiceMessageViews.renderToast(context.getString(R.string.voice_message_release_to_send_toast))
                voiceMessageViews.showRecordingViews()
            }
            RecordingUiState.Cancelled -> {
                stopRecordingTicker()
                voiceMessageViews.hideRecordingViews(recordingState) { callback.onDeleteVoiceMessage() }
                vibrate(context)
            }
            RecordingUiState.Locked    -> {
                voiceMessageViews.renderLocked()
                postDelayed({
                    voiceMessageViews.showRecordingLockedViews(recordingState)
                }, 500)
            }
            RecordingUiState.Playback  -> {
                stopRecordingTicker()
                voiceMessageViews.showPlaybackViews()
            }
            is DraggingState           -> when (recordingState) {
                is DraggingState.Cancelling -> voiceMessageViews.renderCancelling(recordingState.distanceX)
                is DraggingState.Locking    -> {
                    if (previousState is DraggingState.Cancelling) {
                        voiceMessageViews.showRecordingViews()
                    }
                    voiceMessageViews.renderLocking(recordingState.distanceY)
                }
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
        val currentState = lastKnownState ?: return
        voiceMessageViews.renderRecordingTimer(currentState, milliseconds / 1_000)
        val timeDiffToRecordingLimit = BuildConfig.VOICE_MESSAGE_DURATION_LIMIT_MS - milliseconds
        if (timeDiffToRecordingLimit <= 0) {
            post {
                callback.onRecordingLimitReached()
            }
        } else if (timeDiffToRecordingLimit in 10_000..10_999) {
            post {
                val secondsRemaining = floor(timeDiffToRecordingLimit / 1000f).toInt()
                voiceMessageViews.renderToast(context.getString(R.string.voice_message_n_seconds_warning_toast, secondsRemaining))
                vibrate(context)
            }
        }
    }

    private fun stopRecordingTicker() {
        recordingTicker?.stop()
        recordingTicker = null
    }

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
