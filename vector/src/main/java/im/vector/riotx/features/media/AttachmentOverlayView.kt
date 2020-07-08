/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.media

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import im.vector.riotx.R
import im.vector.riotx.attachmentviewer.AttachmentEventListener
import im.vector.riotx.attachmentviewer.AttachmentEvents

class AttachmentOverlayView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), AttachmentEventListener {

    var onShareCallback: (() -> Unit)? = null
    var onBack: (() -> Unit)? = null

    private val counterTextView: TextView
    private val infoTextView: TextView
    private val shareImage: ImageView
    private val overlayPlayPauseButton: ImageView
    private val overlaySeekBar: SeekBar

    val videoControlsGroup: Group

    init {
        View.inflate(context, R.layout.merge_image_attachment_overlay, this)
        setBackgroundColor(Color.TRANSPARENT)
        counterTextView = findViewById(R.id.overlayCounterText)
        infoTextView = findViewById(R.id.overlayInfoText)
        shareImage = findViewById(R.id.overlayShareButton)
        videoControlsGroup = findViewById(R.id.overlayVideoControlsGroup)
        overlayPlayPauseButton = findViewById(R.id.overlayPlayPauseButton)
        overlaySeekBar = findViewById(R.id.overlaySeekBar)

        overlaySeekBar.isEnabled = false
        findViewById<ImageView>(R.id.overlayBackButton).setOnClickListener {
            onBack?.invoke()
        }
        findViewById<ImageView>(R.id.overlayShareButton).setOnClickListener {
            onShareCallback?.invoke()
        }
    }

    fun updateWith(counter: String, senderInfo: String) {
        counterTextView.text = counter
        infoTextView.text = senderInfo
    }

    override fun onEvent(event: AttachmentEvents) {
        when (event) {
            is AttachmentEvents.VideoEvent -> {
                overlayPlayPauseButton.setImageResource(if (!event.isPlaying) R.drawable.ic_play_arrow else R.drawable.ic_pause)
                val safeDuration = (if (event.duration == 0) 100 else event.duration).toFloat()
                val percent = ((event.progress / safeDuration) * 100f).toInt().coerceAtMost(100)
                overlaySeekBar.progress = percent
            }
        }
    }
}
