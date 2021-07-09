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
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.visualizer.amplitude.AudioRecordView
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.hardware.vibrate
import im.vector.app.core.utils.toast
import im.vector.app.databinding.ViewVoiceMessageRecorderBinding
import im.vector.app.features.home.room.detail.timeline.helper.VoiceMessagePlaybackTracker
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
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
    }

    private val views: ViewVoiceMessageRecorderBinding

    var callback: Callback? = null
    var voiceMessagePlaybackTracker: VoiceMessagePlaybackTracker? = null
        set(value) {
            field = value
            value?.track(VoiceMessagePlaybackTracker.RECORDING_ID, this)
        }

    private var recordingState: RecordingState = RecordingState.NONE

    private var firstX: Float = 0f
    private var firstY: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f

    private var recordingTime: Int = 0
    private var amplitudeList = emptyList<Int>()
    private val recordingTimer = Timer()
    private var recordingTimerTask: TimerTask? = null

    init {
        inflate(context, R.layout.view_voice_message_recorder, this)
        views = ViewVoiceMessageRecorderBinding.bind(this)

        initVoiceRecordingViews()
    }

    fun initVoiceRecordingViews() {
        hideRecordingViews(animationDuration = 0)
        stopRecordingTimer()

        views.voiceMessageMicButton.isVisible = true
        views.voiceMessageSendButton.isVisible = false

        views.voiceMessageSendButton.setOnClickListener {
            stopRecordingTimer()
            hideRecordingViews(animationDuration = 0)
            views.voiceMessageSendButton.isVisible = false
            recordingState = RecordingState.NONE
            callback?.onVoiceRecordingEnded(isCancelled = false)
        }

        views.voiceMessageDeletePlayback.setOnClickListener {
            stopRecordingTimer()
            hideRecordingViews(animationDuration = 0)
            views.voiceMessageSendButton.isVisible = false
            recordingState = RecordingState.NONE
            callback?.onVoiceRecordingEnded(isCancelled = true)
        }

        views.voicePlaybackWaveform.setOnClickListener {
            if (recordingState !== RecordingState.PLAYBACK) {
                recordingState = RecordingState.PLAYBACK
                showPlaybackViews()
            }
        }

        views.voicePlaybackControlButton.setOnClickListener {
            callback?.onVoicePlaybackButtonClicked()
        }

        views.voiceMessageMicButton.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecordingTimer()
                    callback?.onVoiceRecordingStarted()
                    recordingState = RecordingState.STARTED
                    showRecordingViews()

                    firstX = event.rawX
                    firstY = event.rawY
                    lastX = firstX
                    lastY = firstY
                    true
                }
                MotionEvent.ACTION_UP   -> {
                    if (recordingState != RecordingState.LOCKED) {
                        stopRecordingTimer()
                        val isCancelled = recordingState == RecordingState.NONE || recordingState == RecordingState.CANCELLED
                        callback?.onVoiceRecordingEnded(isCancelled)
                        recordingState = RecordingState.NONE
                        hideRecordingViews()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    handleMoveAction(event)
                    true
                }
                else                    -> false
            }
        }
    }

    private fun handleMoveAction(event: MotionEvent) {
        val currentX = event.rawX
        val currentY = event.rawY

        val isRecordingStateChanged = updateRecordingState(currentX, currentY)

        when (recordingState) {
            RecordingState.CANCELLING -> {
                val translationAmount = currentX - firstX
                views.voiceMessageMicButton.translationX = translationAmount
                views.voiceMessageSlideToCancel.translationX = translationAmount
                views.voiceMessageSlideToCancel.alpha = 1 - abs(translationAmount) / ((firstX - views.voiceMessageTimer.x) / 3)
                views.voiceMessageLockBackground.isVisible = false
                views.voiceMessageLockImage.isVisible = false
                views.voiceMessageLockArrow.isVisible = false
            }
            RecordingState.LOCKING    -> {
                views.voiceMessageLockImage.setImageResource(R.drawable.ic_voice_message_unlocked)
                val translationAmount = currentY - firstY
                views.voiceMessageMicButton.translationY = translationAmount
                views.voiceMessageLockArrow.translationY = translationAmount
            }
            RecordingState.CANCELLED  -> {
                callback?.onVoiceRecordingEnded(true)
                hideRecordingViews()
            }
            RecordingState.LOCKED     -> {
                if (isRecordingStateChanged) { // Do not update views if it was already in locked state.
                    views.voiceMessageLockImage.setImageResource(R.drawable.ic_voice_message_locked)
                    views.voiceMessageLockImage.postDelayed({
                        showRecordingLockedViews()
                    }, 500)
                }
            }
            RecordingState.STARTED    -> {
                showRecordingViews()
            }
            RecordingState.NONE       -> Timber.d("VoiceMessageRecorderView shouldn't be in NONE state while moving.")
            RecordingState.PLAYBACK   -> Timber.d("VoiceMessageRecorderView shouldn't be in PLAYBACK state while moving.")
        }
        lastX = currentX
        lastY = currentY
    }

    private fun updateRecordingState(currentX: Float, currentY: Float): Boolean {
        val previousRecordingState = recordingState
        val distanceX = abs(firstX - currentX)
        val distanceY = abs(firstY - currentY)
        if (recordingState == RecordingState.STARTED) { // Determine if cancelling or locking for the first move action.
            if (currentX < firstX && distanceX > distanceY) {
                recordingState = RecordingState.CANCELLING
            } else if (currentY < firstY && distanceY > distanceX) {
                recordingState = RecordingState.LOCKING
            }
        } else if (recordingState == RecordingState.CANCELLING) { // Check if cancelling conditions met, also check if it should be initial state
            if (abs(currentX - firstX) < 10 && lastX < currentX) {
                recordingState = RecordingState.STARTED
            } else if (shouldCancelRecording()) {
                recordingState = RecordingState.CANCELLED
            }
        } else if (recordingState == RecordingState.LOCKING) { // Check if locking conditions met, also check if it should be initial state
            if (abs(currentY - firstY) < 10 && lastY < currentY) {
                recordingState = RecordingState.STARTED
            } else if (shouldLockRecording()) {
                recordingState = RecordingState.LOCKED
            }
        }
        return previousRecordingState != recordingState
    }

    private fun shouldCancelRecording(): Boolean {
        return abs(views.voiceMessageTimer.x + views.voiceMessageTimer.width - views.voiceMessageSlideToCancel.x) < 10
                || views.voiceMessageSlideToCancel.x <= views.voiceMessageTimer.x + views.voiceMessageTimer.width // To handle super fast moving
    }

    private fun shouldLockRecording(): Boolean {
        return abs(views.voiceMessageLockImage.y + views.voiceMessageLockImage.height - views.voiceMessageLockArrow.y) < 10
    }

    private fun startRecordingTimer() {
        recordingTimerTask = object : TimerTask() {
            override fun run() {
                recordingTime++
                showRecordingTimer()
                showRecordingWaveform()
                val timeDiffToRecordingLimit = BuildConfig.VOICE_MESSAGE_DURATION_LIMIT_MS - recordingTime * 1000
                if (timeDiffToRecordingLimit <= 0) {
                    views.voiceMessageRecordingLayout.post {
                        recordingState = RecordingState.PLAYBACK
                        showPlaybackViews()
                        stopRecordingTimer()
                    }
                } else if (timeDiffToRecordingLimit in 10000..10999) {
                    views.voiceMessageRecordingLayout.post {
                        context.toast(context.getString(R.string.voice_message_n_seconds_warning_toast, floor(timeDiffToRecordingLimit / 1000f).toInt()))
                        vibrate(context)
                    }
                }
            }
        }
        recordingTimer.scheduleAtFixedRate(recordingTimerTask, 0, 1000)
    }

    private fun showRecordingTimer() {
        val formattedTimerText = DateUtils.formatElapsedTime((recordingTime).toLong())
        if (recordingState == RecordingState.LOCKED) {
            views.voicePlaybackTime.apply {
                post {
                    text = formattedTimerText
                }
            }
        } else {
            views.voiceMessageTimer.post {
                views.voiceMessageTimer.text = formattedTimerText
            }
        }
    }

    private fun showRecordingWaveform() {
        val audioRecordView = views.voicePlaybackWaveform
        audioRecordView.apply {
            post {
                recreate()
                amplitudeList.toMutableList().forEach { amplitude ->
                    update(amplitude)
                }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerTask?.cancel()
        recordingTime = 0
    }

    private fun showRecordingViews() {
        views.voiceMessageMicButton.setImageResource(R.drawable.ic_voice_mic_recording)
        (views.voiceMessageMicButton.layoutParams as MarginLayoutParams).apply { setMargins(0, 0, 0, 0) }
        views.voiceMessageLockBackground.isVisible = true
        views.voiceMessageLockBackground.animate().setDuration(300).translationY(-dpToPx(148)).start()
        views.voiceMessageLockImage.isVisible = true
        views.voiceMessageLockImage.setImageResource(R.drawable.ic_voice_message_unlocked)
        views.voiceMessageLockImage.animate().setDuration(500).translationY(-dpToPx(148)).start()
        views.voiceMessageLockArrow.isVisible = true
        views.voiceMessageSlideToCancel.isVisible = true
        views.voiceMessageTimerIndicator.isVisible = true
        views.voiceMessageTimer.isVisible = true
        views.voiceMessageSlideToCancel.alpha = 1f
        views.voiceMessageSendButton.isVisible = false
    }

    private fun hideRecordingViews(animationDuration: Int = 300) {
        views.voiceMessageMicButton.setImageResource(R.drawable.ic_voice_mic)
        views.voiceMessageMicButton.animate().translationX(0f).translationY(0f).setDuration(animationDuration.toLong()).setDuration(0).start()
        (views.voiceMessageMicButton.layoutParams as MarginLayoutParams).apply { setMargins(0, 0, dpToPx(12).toInt(), dpToPx(12).toInt()) }
        views.voiceMessageLockBackground.isVisible = false
        views.voiceMessageLockBackground.animate().translationY(dpToPx(0)).start()
        views.voiceMessageLockImage.isVisible = false
        views.voiceMessageLockImage.animate().translationY(dpToPx(0)).start()
        views.voiceMessageLockArrow.isVisible = false
        views.voiceMessageLockArrow.animate().translationY(0f).start()
        views.voiceMessageSlideToCancel.isVisible = false
        views.voiceMessageSlideToCancel.animate().translationX(0f).translationY(0f).start()
        views.voiceMessageTimerIndicator.isVisible = false
        views.voiceMessageTimer.isVisible = false
        views.voiceMessagePlaybackLayout.isVisible = false
    }

    private fun showRecordingLockedViews() {
        hideRecordingViews(animationDuration = 0)
        views.voiceMessagePlaybackLayout.isVisible = true
        views.voiceMessagePlaybackTimerIndicator.isVisible = true
        views.voicePlaybackControlButton.isVisible = false
        views.voiceMessageSendButton.isVisible = true
        context.toast(R.string.voice_message_tap_to_stop_toast)
    }

    private fun showPlaybackViews() {
        views.voiceMessagePlaybackTimerIndicator.isVisible = false
        views.voicePlaybackControlButton.isVisible = true
        callback?.onVoiceRecordingPlaybackModeOn()
    }

    @Px
    private fun dpToPx(dp: Int): Float {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
        )
    }

    private enum class RecordingState {
        NONE,
        STARTED,
        CANCELLING,
        CANCELLED,
        LOCKING,
        LOCKED,
        PLAYBACK
    }

    override fun onUpdate(state: VoiceMessagePlaybackTracker.Listener.State) {
        when (state) {
            is VoiceMessagePlaybackTracker.Listener.State.Recording    -> {
                this.amplitudeList = state.amplitudeList
            }
            is VoiceMessagePlaybackTracker.Listener.State.Playing  -> {
                views.voicePlaybackControlButton.setImageResource(R.drawable.ic_play_pause_pause)
                val formattedTimerText = DateUtils.formatElapsedTime((state.playbackTime / 1000).toLong())
                views.voicePlaybackTime.setText(formattedTimerText)
            }
            is VoiceMessagePlaybackTracker.Listener.State.Idle -> {
                views.voicePlaybackControlButton.setImageResource(R.drawable.ic_play_pause_play)
            }
        }
    }
}
