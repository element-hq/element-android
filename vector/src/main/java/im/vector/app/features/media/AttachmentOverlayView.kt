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

package im.vector.app.features.media

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import im.vector.app.R
import im.vector.lib.attachmentviewer.AttachmentEventListener
import im.vector.lib.attachmentviewer.AttachmentEvents

class AttachmentOverlayView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), AttachmentEventListener {

    var onShareCallback: (() -> Unit)? = null
    var onBack: (() -> Unit)? = null
    var onPlayPause: ((play: Boolean) -> Unit)? = null
    var videoSeekTo: ((progress: Int) -> Unit)? = null

    private val counterTextView: TextView
    private val infoTextView: TextView
    private val shareImage: ImageView
    private val overlayPlayPauseButton: ImageView
    private val overlaySeekBar: SeekBar

    var isPlaying = false

    val videoControlsGroup: Group

    var suspendSeekBarUpdate = false

    init {
        View.inflate(context, R.layout.merge_image_attachment_overlay, this)
        setBackgroundColor(Color.TRANSPARENT)
        counterTextView = findViewById(R.id.overlayCounterText)
        infoTextView = findViewById(R.id.overlayInfoText)
        shareImage = findViewById(R.id.overlayShareButton)
        videoControlsGroup = findViewById(R.id.overlayVideoControlsGroup)
        overlayPlayPauseButton = findViewById(R.id.overlayPlayPauseButton)
        overlaySeekBar = findViewById(R.id.overlaySeekBar)
        findViewById<ImageView>(R.id.overlayBackButton).setOnClickListener {
            onBack?.invoke()
        }
        findViewById<ImageView>(R.id.overlayShareButton).setOnClickListener {
            onShareCallback?.invoke()
        }
        findViewById<ImageView>(R.id.overlayPlayPauseButton).setOnClickListener {
            onPlayPause?.invoke(!isPlaying)
        }

        overlaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoSeekTo?.invoke(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                suspendSeekBarUpdate = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                suspendSeekBarUpdate = false
            }
        })
    }

    fun updateWith(counter: String, senderInfo: String) {
        counterTextView.text = counter
        infoTextView.text = senderInfo
    }

    override fun onEvent(event: AttachmentEvents) {
        when (event) {
            is AttachmentEvents.VideoEvent -> {
                overlayPlayPauseButton.setImageResource(if (!event.isPlaying) R.drawable.ic_play_arrow else R.drawable.ic_pause)
                if (!suspendSeekBarUpdate) {
                    val safeDuration = (if (event.duration == 0) 100 else event.duration).toFloat()
                    val percent = ((event.progress / safeDuration) * 100f).toInt().coerceAtMost(100)
                    isPlaying = event.isPlaying
                    overlaySeekBar.progress = percent
                }
            }
        }
    }
}
