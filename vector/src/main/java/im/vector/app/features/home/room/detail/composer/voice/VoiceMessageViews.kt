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

import android.annotation.SuppressLint
import android.content.res.Resources
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import im.vector.app.R
import im.vector.app.core.extensions.setAttributeBackground
import im.vector.app.core.extensions.setAttributeTintedBackground
import im.vector.app.core.extensions.setAttributeTintedImageResource
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewVoiceMessageRecorderBinding
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView.RecordingState
import im.vector.app.features.home.room.detail.timeline.helper.VoiceMessagePlaybackTracker
import org.matrix.android.sdk.api.extensions.orFalse

class VoiceMessageViews(
        private val resources: Resources,
        private val views: ViewVoiceMessageRecorderBinding,
        private val dimensionConverter: DimensionConverter,
) {

    private val distanceToLock = dimensionConverter.dpToPx(48).toFloat()
    private val distanceToCancel = dimensionConverter.dpToPx(120).toFloat()
    private val rtlXMultiplier = resources.getInteger(R.integer.rtl_x_multiplier)

    fun start(actions: Actions) {
        views.voiceMessageSendButton.setOnClickListener {
            views.voiceMessageSendButton.isVisible = false
            actions.sendMessage()
        }

        views.voiceMessageDeletePlayback.setOnClickListener {
            views.voiceMessageSendButton.isVisible = false
            actions.delete()
        }

        views.voicePlaybackWaveform.setOnClickListener {
            actions.waveformClicked()
        }

        views.voicePlaybackControlButton.setOnClickListener {
            actions.onVoicePlaybackButtonClicked()
        }
        observeMicButton(actions)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeMicButton(actions: Actions) {
        val positions = DraggableStateProcessor(resources, dimensionConverter)
        views.voiceMessageMicButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    positions.reset(event)
                    actions.onRequestRecording()
                    true
                }
                MotionEvent.ACTION_UP   -> {
                    actions.onRecordingStopped()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (actions.isActive()) {
                        actions.updateState { currentState -> positions.process(event, currentState) }
                        true
                    } else {
                        false
                    }
                }
                else                    -> false
            }
        }
    }

    fun renderStarted(distanceX: Float) {
        val translationAmount = distanceX.coerceAtMost(distanceToCancel)
        views.voiceMessageMicButton.translationX = -translationAmount * rtlXMultiplier
        views.voiceMessageSlideToCancel.translationX = -translationAmount / 2 * rtlXMultiplier
    }

    fun renderLocked() {
        views.voiceMessageLockImage.setImageResource(R.drawable.ic_voice_message_locked)
    }

    fun renderLocking(distanceY: Float) {
        views.voiceMessageLockImage.setAttributeTintedImageResource(R.drawable.ic_voice_message_locked, R.attr.colorPrimary)
        val translationAmount = -distanceY.coerceIn(0F, distanceToLock)
        views.voiceMessageMicButton.translationY = translationAmount
        views.voiceMessageLockArrow.translationY = translationAmount
        views.voiceMessageLockArrow.alpha = 1 - (-translationAmount / distanceToLock)
        // Reset X translations
        views.voiceMessageMicButton.translationX = 0F
        views.voiceMessageSlideToCancel.translationX = 0F
    }

    fun renderCancelling(distanceX: Float) {
        val translationAmount = distanceX.coerceAtMost(distanceToCancel)
        views.voiceMessageMicButton.translationX = -translationAmount * rtlXMultiplier
        views.voiceMessageSlideToCancel.translationX = -translationAmount / 2 * rtlXMultiplier
        val reducedAlpha = (1 - translationAmount / distanceToCancel / 1.5).toFloat()
        views.voiceMessageSlideToCancel.alpha = reducedAlpha
        views.voiceMessageTimerIndicator.alpha = reducedAlpha
        views.voiceMessageTimer.alpha = reducedAlpha
        views.voiceMessageLockBackground.isVisible = false
        views.voiceMessageLockImage.isVisible = false
        views.voiceMessageLockArrow.isVisible = false
        // Reset Y translations
        views.voiceMessageMicButton.translationY = 0F
        views.voiceMessageLockArrow.translationY = 0F
    }

    fun showRecordingViews() {
        views.voiceMessageMicButton.setImageResource(R.drawable.ic_voice_mic_recording)
        views.voiceMessageMicButton.setAttributeTintedBackground(R.drawable.circle_with_halo, R.attr.colorPrimary)
        views.voiceMessageMicButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(0, 0, 0, 0)
        }
        views.voiceMessageMicButton.animate().scaleX(1.5f).scaleY(1.5f).setDuration(300).start()

        views.voiceMessageLockBackground.isVisible = true
        views.voiceMessageLockBackground.animate().setDuration(300).translationY(-dimensionConverter.dpToPx(180).toFloat()).start()
        views.voiceMessageLockImage.isVisible = true
        views.voiceMessageLockImage.setImageResource(R.drawable.ic_voice_message_unlocked)
        views.voiceMessageLockImage.animate().setDuration(500).translationY(-dimensionConverter.dpToPx(180).toFloat()).start()
        views.voiceMessageLockArrow.isVisible = true
        views.voiceMessageLockArrow.alpha = 1f
        views.voiceMessageSlideToCancel.isVisible = true
        views.voiceMessageTimerIndicator.isVisible = true
        views.voiceMessageTimer.isVisible = true
        views.voiceMessageSlideToCancel.alpha = 1f
        views.voiceMessageTimerIndicator.alpha = 1f
        views.voiceMessageTimer.alpha = 1f
        views.voiceMessageSendButton.isVisible = false
    }

    fun hideRecordingViews(recordingState: RecordingState, isCancelled: Boolean?, onVoiceRecordingEnded: (Boolean) -> Unit) {
        // We need to animate the lock image first
        if (recordingState != RecordingState.Locked || isCancelled.orFalse()) {
            views.voiceMessageLockImage.isVisible = false
            views.voiceMessageLockImage.animate().translationY(0f).start()
            views.voiceMessageLockBackground.isVisible = false
            views.voiceMessageLockBackground.animate().translationY(0f).start()
        } else {
            animateLockImageWithBackground()
        }
        views.voiceMessageLockArrow.isVisible = false
        views.voiceMessageLockArrow.animate().translationY(0f).start()
        views.voiceMessageSlideToCancel.isVisible = false
        views.voiceMessageSlideToCancel.animate().translationX(0f).translationY(0f).start()
        views.voiceMessagePlaybackLayout.isVisible = false

        if (recordingState != RecordingState.Locked) {
            views.voiceMessageMicButton
                    .animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(150)
                    .withEndAction {
                        views.voiceMessageTimerIndicator.isVisible = false
                        views.voiceMessageTimer.isVisible = false
                        resetMicButtonUi()
                        isCancelled?.let {
                            onVoiceRecordingEnded(it)
                        }
                    }
                    .start()
        } else {
            views.voiceMessageTimerIndicator.isVisible = false
            views.voiceMessageTimer.isVisible = false
            views.voiceMessageMicButton.apply {
                scaleX = 1f
                scaleY = 1f
                translationX = 0f
                translationY = 0f
            }
            isCancelled?.let {
                onVoiceRecordingEnded(it)
            }
        }

        // Hide toasts if user cancelled recording before the timeout of the toast.
        if (recordingState == RecordingState.Cancelled || recordingState == RecordingState.None) {
            hideToast()
        }
    }

    fun animateLockImageWithBackground() {
        views.voiceMessageLockBackground.updateLayoutParams {
            height = dimensionConverter.dpToPx(78)
        }
        views.voiceMessageLockBackground.apply {
            animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(400L)
                    .withEndAction {
                        updateLayoutParams {
                            height = dimensionConverter.dpToPx(180)
                        }
                        isVisible = false
                        scaleX = 1f
                        scaleY = 1f
                        animate().translationY(0f).start()
                    }
                    .start()
        }

        // Lock image animation
        views.voiceMessageMicButton.isInvisible = true
        views.voiceMessageLockImage.apply {
            isVisible = true
            animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(400L)
                    .withEndAction {
                        isVisible = false
                        scaleX = 1f
                        scaleY = 1f
                        translationY = 0f
                        resetMicButtonUi()
                    }
                    .start()
        }
    }

    fun resetMicButtonUi() {
        views.voiceMessageMicButton.isVisible = true
        views.voiceMessageMicButton.setImageResource(R.drawable.ic_voice_mic)
        views.voiceMessageMicButton.setAttributeBackground(android.R.attr.selectableItemBackgroundBorderless)
        views.voiceMessageMicButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            if (rtlXMultiplier == -1) {
                // RTL
                setMargins(dimensionConverter.dpToPx(12), 0, 0, dimensionConverter.dpToPx(12))
            } else {
                setMargins(0, 0, dimensionConverter.dpToPx(12), dimensionConverter.dpToPx(12))
            }
        }
    }

    fun hideToast() {
        views.voiceMessageToast.isVisible = false
    }

    fun showRecordingLockedViews(recordingState: RecordingState, onVoiceRecordingEnded: (Boolean) -> Unit) {
        hideRecordingViews(recordingState, null, onVoiceRecordingEnded)
        views.voiceMessagePlaybackLayout.isVisible = true
        views.voiceMessagePlaybackTimerIndicator.isVisible = true
        views.voicePlaybackControlButton.isVisible = false
        views.voiceMessageSendButton.isVisible = true
        views.voicePlaybackWaveform.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        renderToast(resources.getString(R.string.voice_message_tap_to_stop_toast))
    }

    fun showPlaybackViews() {
        views.voiceMessagePlaybackTimerIndicator.isVisible = false
        views.voicePlaybackControlButton.isVisible = true
        views.voicePlaybackWaveform.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun initViews(onVoiceRecordingEnded: (Boolean) -> Unit) {
        hideRecordingViews(RecordingState.None, null, onVoiceRecordingEnded)
        views.voiceMessageMicButton.isVisible = true
        views.voiceMessageSendButton.isVisible = false
        views.voicePlaybackWaveform.post { views.voicePlaybackWaveform.recreate() }
    }

    fun renderPlaying(state: VoiceMessagePlaybackTracker.Listener.State.Playing) {
        views.voicePlaybackControlButton.setImageResource(R.drawable.ic_play_pause_pause)
        views.voicePlaybackControlButton.contentDescription = resources.getString(R.string.a11y_pause_voice_message)
        val formattedTimerText = DateUtils.formatElapsedTime((state.playbackTime / 1000).toLong())
        views.voicePlaybackTime.text = formattedTimerText
    }

    fun renderIdle() {
        views.voicePlaybackControlButton.setImageResource(R.drawable.ic_play_pause_play)
        views.voicePlaybackControlButton.contentDescription = resources.getString(R.string.a11y_play_voice_message)
    }

    fun renderToast(message: String) {
        views.voiceMessageToast.removeCallbacks(hideToastRunnable)
        views.voiceMessageToast.text = message
        views.voiceMessageToast.isVisible = true
        views.voiceMessageToast.postDelayed(hideToastRunnable, 2_000)
    }

    private val hideToastRunnable = Runnable {
        views.voiceMessageToast.isVisible = false
    }

    fun renderRecordingTimer(recordingState: RecordingState, recordingTimeMillis: Long) {
        val formattedTimerText = DateUtils.formatElapsedTime(recordingTimeMillis)
        if (recordingState == RecordingState.Locked) {
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

    fun renderRecordingWaveform(amplitudeList: Array<Int>) {
        views.voicePlaybackWaveform.post {
            views.voicePlaybackWaveform.apply {
                amplitudeList.iterator().forEach {
                    update(it)
                }
            }
        }
    }

    fun renderVisibilityChanged(parentChanged: Boolean, visibility: Int) {
        if (parentChanged && visibility == ConstraintLayout.VISIBLE) {
            views.voiceMessageMicButton.contentDescription = resources.getString(R.string.a11y_start_voice_message)
        } else {
            views.voiceMessageMicButton.contentDescription = ""
        }
    }

    interface Actions {
        fun onRequestRecording()
        fun onRecordingStopped()
        fun isActive(): Boolean
        fun updateState(updater: (RecordingState) -> RecordingState)
        fun sendMessage()
        fun delete()
        fun waveformClicked()
        fun onVoicePlaybackButtonClicked()
    }
}
