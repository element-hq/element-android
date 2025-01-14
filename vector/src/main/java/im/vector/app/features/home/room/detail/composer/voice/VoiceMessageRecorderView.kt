/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.voice

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.hardware.vibrate
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewVoiceMessageRecorderBinding
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.core.utils.timer.CountUpTimer
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject
import kotlin.math.floor

/**
 * Encapsulates the voice message recording view and animations.
 */
@AndroidEntryPoint
class VoiceMessageRecorderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), AudioMessagePlaybackTracker.Listener {

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
        fun onVoiceWaveformTouchedUp(percentage: Float, duration: Int)
        fun onVoiceWaveformMoved(percentage: Float, duration: Int)
    }

    @Inject lateinit var clock: Clock
    @Inject lateinit var voiceMessageConfig: VoiceMessageConfig

    // We need to define views as lateinit var to be able to check if initialized for the bug fix on api 21 and 22.
    @Suppress("UNNECESSARY_LATEINIT")
    private lateinit var voiceMessageViews: VoiceMessageViews
    lateinit var callback: Callback

    private var recordingTicker: CountUpTimer? = null
    private var lastKnownState: RecordingUiState? = null
    private var dragState: DraggingState = DraggingState.Ignored
    private var recordingDuration: Long = 0

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
                    DraggingState.Lock -> {
                        // do nothing,
                        // onSendVoiceMessage, onDeleteVoiceMessage or onRecordingLimitReached will be triggered instead
                    }
                    DraggingState.Cancel -> callback.onVoiceRecordingCancelled()
                    else -> callback.onVoiceRecordingEnded()
                }
            }

            override fun onSendVoiceMessage() = callback.onSendVoiceMessage()
            override fun onDeleteVoiceMessage() = callback.onDeleteVoiceMessage()
            override fun onWaveformClicked() {
                when (lastKnownState) {
                    is RecordingUiState.Recording,
                    is RecordingUiState.Locked -> callback.onRecordingWaveformClicked()
                    else -> Unit
                }
            }

            override fun onVoicePlaybackButtonClicked() = callback.onVoicePlaybackButtonClicked()
            override fun onMicButtonDrag(nextDragStateCreator: (DraggingState) -> DraggingState) {
                onDrag(dragState, newDragState = nextDragStateCreator(dragState))
            }

            override fun onVoiceWaveformTouchedUp(percentage: Float) {
                if (lastKnownState == RecordingUiState.Draft) {
                    callback.onVoiceWaveformTouchedUp(percentage, recordingDuration.toInt())
                }
            }

            override fun onVoiceWaveformMoved(percentage: Float) {
                if (lastKnownState == RecordingUiState.Draft) {
                    callback.onVoiceWaveformMoved(percentage, recordingDuration.toInt())
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

    fun render(recordingState: RecordingUiState) {
        if (lastKnownState == recordingState) return
        when (recordingState) {
            RecordingUiState.Idle -> {
                reset()
            }
            is RecordingUiState.Recording -> {
                startRecordingTicker(startFromLocked = false, startAt = recordingState.recordingStartTimestamp)
                voiceMessageViews.renderToast(context.getString(CommonStrings.voice_message_release_to_send_toast))
                voiceMessageViews.showRecordingViews()
                dragState = DraggingState.Ready
            }
            is RecordingUiState.Locked -> {
                if (lastKnownState == null) {
                    startRecordingTicker(startFromLocked = true, startAt = recordingState.recordingStartTimestamp)
                }
                voiceMessageViews.renderLocked()
                postDelayed({
                    voiceMessageViews.showRecordingLockedViews(recordingState)
                }, 500)
            }
            RecordingUiState.Draft -> {
                stopRecordingTicker()
                voiceMessageViews.showDraftViews()
            }
        }
        lastKnownState = recordingState
    }

    private fun reset() {
        stopRecordingTicker()
        voiceMessageViews.initViews()
        dragState = DraggingState.Ignored
    }

    private fun onDrag(currentDragState: DraggingState, newDragState: DraggingState) {
        if (currentDragState == newDragState) return
        when (newDragState) {
            is DraggingState.Cancelling -> voiceMessageViews.renderCancelling(newDragState.distanceX)
            is DraggingState.Locking -> {
                if (currentDragState is DraggingState.Cancelling) {
                    voiceMessageViews.showRecordingViews()
                }
                voiceMessageViews.renderLocking(newDragState.distanceY)
            }
            DraggingState.Cancel -> callback.onVoiceRecordingCancelled()
            DraggingState.Lock -> callback.onVoiceRecordingLocked()
            DraggingState.Ignored,
            DraggingState.Ready -> {
                // do nothing
            }
        }
        dragState = newDragState
    }

    private fun startRecordingTicker(startFromLocked: Boolean, startAt: Long) {
        val startMs = ((clock.epochMillis() - startAt)).coerceAtLeast(0)
        recordingTicker?.stop()
        recordingTicker = CountUpTimer().apply {
            tickListener = CountUpTimer.TickListener { milliseconds ->
                val isLocked = startFromLocked || lastKnownState is RecordingUiState.Locked
                onRecordingTick(isLocked, milliseconds + startMs)
            }
            start()
        }
        onRecordingTick(startFromLocked, milliseconds = startMs)
    }

    private fun onRecordingTick(isLocked: Boolean, milliseconds: Long) {
        voiceMessageViews.renderRecordingTimer(isLocked, milliseconds / 1_000)
        val timeDiffToRecordingLimit = voiceMessageConfig.lengthLimitMs - milliseconds
        if (timeDiffToRecordingLimit <= 0) {
            post {
                callback.onRecordingLimitReached()
            }
        } else if (timeDiffToRecordingLimit in 10_000..10_999) {
            post {
                val secondsRemaining = floor(timeDiffToRecordingLimit / 1000f).toInt()
                voiceMessageViews.renderToast(context.getString(CommonStrings.voice_message_n_seconds_warning_toast, secondsRemaining))
                vibrate(context)
            }
        }
    }

    private fun stopRecordingTicker() {
        recordingDuration = recordingTicker?.elapsedTime() ?: 0
        recordingTicker?.stop()
        recordingTicker = null
    }

    override fun onUpdate(state: AudioMessagePlaybackTracker.Listener.State) {
        when (state) {
            is AudioMessagePlaybackTracker.Listener.State.Recording -> {
                voiceMessageViews.renderRecordingWaveform(state.amplitudeList.toList())
            }
            is AudioMessagePlaybackTracker.Listener.State.Playing -> {
                voiceMessageViews.renderPlaying(state)
            }
            is AudioMessagePlaybackTracker.Listener.State.Paused,
            is AudioMessagePlaybackTracker.Listener.State.Error,
            is AudioMessagePlaybackTracker.Listener.State.Idle -> {
                voiceMessageViews.renderIdle()
            }
        }
    }

    sealed interface RecordingUiState {
        object Idle : RecordingUiState
        data class Recording(val recordingStartTimestamp: Long) : RecordingUiState
        data class Locked(val recordingStartTimestamp: Long) : RecordingUiState
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
