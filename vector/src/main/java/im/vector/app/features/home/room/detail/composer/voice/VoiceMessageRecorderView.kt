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
        fun onVoiceRecordingLocked()
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
    private var dragState: DraggingState = DraggingState.Ignored

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
                when (dragState) {
                    DraggingState.Lock   -> {
                        // do nothing,
                        // onSendVoiceMessage, onDeleteVoiceMessage or onRecordingLimitReached will be triggered instead
                    }
                    DraggingState.Cancel -> callback.onVoiceRecordingCancelled()
                    else                 -> callback.onVoiceRecordingEnded()
                }
            }

            override fun onSendVoiceMessage() = callback.onSendVoiceMessage()
            override fun onDeleteVoiceMessage() = callback.onDeleteVoiceMessage()
            override fun onWaveformClicked() {
                when (lastKnownState) {
                    RecordingUiState.Draft  -> callback.onVoicePlaybackButtonClicked()
                    RecordingUiState.Started,
                    RecordingUiState.Locked -> callback.onRecordingWaveformClicked()
                }
            }

            override fun onVoicePlaybackButtonClicked() = callback.onVoicePlaybackButtonClicked()
            override fun onMicButtonDrag(nextDragStateCreator: (DraggingState) -> DraggingState) {
                onDrag(dragState, newDragState = nextDragStateCreator(dragState))
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

    fun render(recordingState: RecordingUiState) {
        if (lastKnownState == recordingState) return
        lastKnownState = recordingState
        when (recordingState) {
            RecordingUiState.None      -> {
                reset()
            }
            RecordingUiState.Started   -> {
                startRecordingTicker()
                voiceMessageViews.renderToast(context.getString(R.string.voice_message_release_to_send_toast))
                voiceMessageViews.showRecordingViews()
                dragState = DraggingState.Ready
            }
            RecordingUiState.Cancelled -> {
                reset()
                vibrate(context)
            }
            RecordingUiState.Locked    -> {
                voiceMessageViews.renderLocked()
                postDelayed({
                    voiceMessageViews.showRecordingLockedViews(recordingState)
                }, 500)
            }
            RecordingUiState.Draft     -> {
                stopRecordingTicker()
                voiceMessageViews.showDraftViews()
            }
        }
    }

    private fun reset() {
        stopRecordingTicker()
        voiceMessageViews.initViews()
        dragState = DraggingState.Ignored
    }

    private fun onDrag(currentDragState: DraggingState, newDragState: DraggingState) {
        when (newDragState) {
            is DraggingState.Cancelling -> voiceMessageViews.renderCancelling(newDragState.distanceX)
            is DraggingState.Locking    -> {
                if (currentDragState is DraggingState.Cancelling) {
                    voiceMessageViews.showRecordingViews()
                }
                voiceMessageViews.renderLocking(newDragState.distanceY)
            }
            DraggingState.Cancel        -> callback.onVoiceRecordingCancelled()
            DraggingState.Lock          -> callback.onVoiceRecordingLocked()
            DraggingState.Ignored,
            DraggingState.Ready         -> {
                // do nothing
            }
        }.exhaustive
        dragState = newDragState
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
        object Draft : RecordingUiState
    }

    sealed interface DraggingState {
        object Ready : DraggingState
        object Ignored : DraggingState
        data class Cancelling(val distanceX: Float) : DraggingState
        data class Locking(val distanceY: Float) : DraggingState
        object Cancel : DraggingState
        object Lock : DraggingState
    }
}
