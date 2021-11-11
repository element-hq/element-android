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
import org.matrix.android.sdk.api.extensions.orFalse
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
        // Return true if the recording is started
        fun onVoiceRecordingStarted(): Boolean
        fun onVoiceRecordingEnded(isCancelled: Boolean)
        fun onVoiceRecordingPlaybackModeOn()
        fun onVoicePlaybackButtonClicked()
    }

    // We need to define views as lateinit var to be able to check if initialized for the bug fix on api 21 and 22.
    @Suppress("UNNECESSARY_LATEINIT")
    private lateinit var voiceMessageViews: VoiceMessageViews

    var callback: Callback? = null

    private var recordingState: RecordingState = RecordingState.None
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
        recordingState = RecordingState.None
        stopRecordingTicker()
        voiceMessageViews.initViews(onVoiceRecordingEnded = {})
    }

    private fun initListeners() {
        voiceMessageViews.start(object : VoiceMessageViews.Actions {
            override fun onRequestRecording() {
                if (callback?.onVoiceRecordingStarted().orFalse()) {
                    display(RecordingState.Started)
                }
            }

            override fun onRecordingStopped() {
                if (recordingState != RecordingState.Locked && recordingState != RecordingState.None) {
                    display(RecordingState.None)
                }
            }

            override fun isActive() = recordingState != RecordingState.Cancelled

            override fun updateState(updater: (RecordingState) -> RecordingState) {
                updater(recordingState).also {
                    display(it)
                }
            }

            override fun sendMessage() {
                display(RecordingState.None)
            }

            override fun delete() {
                // this was previously marked as cancelled true
                display(RecordingState.None)
            }

            override fun waveformClicked() {
                display(RecordingState.Playback)
            }

            override fun onVoicePlaybackButtonClicked() {
                callback?.onVoicePlaybackButtonClicked()
            }
        })
    }

    fun display(recordingState: RecordingState) {
        val previousState = this.recordingState
        val stateHasChanged = recordingState != this.recordingState
        this.recordingState = recordingState

        if (stateHasChanged) {
            when (recordingState) {
                RecordingState.None      -> {
                    val isCancelled = previousState == RecordingState.Cancelled
                    voiceMessageViews.hideRecordingViews(recordingState, isCancelled = isCancelled) { callback?.onVoiceRecordingEnded(it) }
                    stopRecordingTicker()
                }
                RecordingState.Started   -> {
                    startRecordingTicker()
                    voiceMessageViews.renderToast(context.getString(R.string.voice_message_release_to_send_toast))
                    voiceMessageViews.showRecordingViews()
                }
                RecordingState.Cancelled -> {
                    voiceMessageViews.hideRecordingViews(recordingState, isCancelled = true) { callback?.onVoiceRecordingEnded(it) }
                    vibrate(context)
                }
                RecordingState.Locked    -> {
                    voiceMessageViews.renderLocked()
                    postDelayed({
                        voiceMessageViews.showRecordingLockedViews(recordingState) { callback?.onVoiceRecordingEnded(it) }
                    }, 500)
                }
                RecordingState.Playback  -> {
                    stopRecordingTicker()
                    voiceMessageViews.showPlaybackViews()
                    callback?.onVoiceRecordingPlaybackModeOn()
                }
                is DraggingState         -> when (recordingState) {
                    is DraggingState.Cancelling -> voiceMessageViews.renderCancelling(recordingState.distanceX)
                    is DraggingState.Locking    -> voiceMessageViews.renderLocking(recordingState.distanceY)
                }.exhaustive
            }
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
        voiceMessageViews.renderRecordingTimer(recordingState, milliseconds / 1_000)
        val timeDiffToRecordingLimit = BuildConfig.VOICE_MESSAGE_DURATION_LIMIT_MS - milliseconds
        if (timeDiffToRecordingLimit <= 0) {
            post {
                display(RecordingState.Playback)
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
    fun isActive() = recordingState !in listOf(RecordingState.None, RecordingState.Cancelled)

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

    sealed interface RecordingState {
        object None : RecordingState
        object Started : RecordingState
        object Cancelled : RecordingState
        object Locked : RecordingState
        object Playback : RecordingState
    }

    sealed interface DraggingState : RecordingState {
        data class Cancelling(val distanceX: Float) : DraggingState
        data class Locking(val distanceY: Float) : DraggingState
    }
}

