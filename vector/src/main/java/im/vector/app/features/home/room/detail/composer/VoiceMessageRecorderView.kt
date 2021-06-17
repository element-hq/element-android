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
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.devlomi.record_view.OnRecordListener
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.databinding.ViewVoiceMessageRecorderBinding
import org.matrix.android.sdk.api.extensions.orFalse

/**
 * Encapsulates the voice message recording view and animations.
 */
class VoiceMessageRecorderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onVoiceRecordingStarted()
        fun onVoiceRecordingEnded(recordTime: Long)
        fun checkVoiceRecordingPermission(): Boolean
    }

    private val views: ViewVoiceMessageRecorderBinding

    var callback: Callback? = null

    init {
        inflate(context, R.layout.view_voice_message_recorder, this)
        views = ViewVoiceMessageRecorderBinding.bind(this)

        views.voiceMessageButton.setRecordView(views.voiceMessageRecordView)
        views.voiceMessageRecordView.timeLimit = BuildConfig.VOICE_MESSAGE_DURATION_LIMIT_MS

        views.voiceMessageRecordView.setRecordPermissionHandler { callback?.checkVoiceRecordingPermission().orFalse() }

        views.voiceMessageRecordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                onVoiceRecordingStarted()
            }

            override fun onCancel() {
                onVoiceRecordingEnded(0)
            }

            override fun onFinish(recordTime: Long, limitReached: Boolean) {
                onVoiceRecordingEnded(recordTime)
            }

            override fun onLessThanSecond() {
                onVoiceRecordingEnded(0)
            }
        })
    }

    private fun onVoiceRecordingStarted() {
        views.voiceMessageLockBackground.isVisible = true
        views.voiceMessageLockArrow.isVisible = true
        views.voiceMessageLockImage.isVisible = true
        views.voiceMessageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_voice_mic_recording))
        callback?.onVoiceRecordingStarted()
    }

    private fun onVoiceRecordingEnded(recordTime: Long) {
        views.voiceMessageLockBackground.isVisible = false
        views.voiceMessageLockArrow.isVisible = false
        views.voiceMessageLockImage.isVisible = false
        views.voiceMessageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_voice_mic))
        callback?.onVoiceRecordingEnded(recordTime)
    }
}
