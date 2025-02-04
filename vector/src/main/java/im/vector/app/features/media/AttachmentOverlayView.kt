/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.app.R
import im.vector.app.databinding.MergeImageAttachmentOverlayBinding
import im.vector.lib.attachmentviewer.AttachmentEventListener
import im.vector.lib.attachmentviewer.AttachmentEvents

class AttachmentOverlayView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), AttachmentEventListener {

    var interactionListener: AttachmentInteractionListener? = null
    val views: MergeImageAttachmentOverlayBinding

    private var isPlaying = false
    private var suspendSeekBarUpdate = false

    init {
        inflate(context, R.layout.merge_image_attachment_overlay, this)
        views = MergeImageAttachmentOverlayBinding.bind(this)
        setBackgroundColor(Color.TRANSPARENT)
        views.overlayBackButton.setOnClickListener {
            interactionListener?.onDismiss()
        }
        views.overlayShareButton.setOnClickListener {
            interactionListener?.onShare()
        }
        views.overlayDownloadButton.setOnClickListener {
            interactionListener?.onDownload()
        }
        views.overlayPlayPauseButton.setOnClickListener {
            interactionListener?.onPlayPause(!isPlaying)
        }

        views.overlaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    interactionListener?.videoSeekTo(progress)
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
        views.overlayCounterText.text = counter
        views.overlayInfoText.text = senderInfo
    }

    override fun onEvent(event: AttachmentEvents) {
        when (event) {
            is AttachmentEvents.VideoEvent -> {
                views.overlayPlayPauseButton.setImageResource(if (!event.isPlaying) R.drawable.ic_play_arrow else R.drawable.ic_pause)
                if (!suspendSeekBarUpdate) {
                    val safeDuration = (if (event.duration == 0) 100 else event.duration).toFloat()
                    val percent = ((event.progress / safeDuration) * 100f).toInt().coerceAtMost(100)
                    isPlaying = event.isPlaying
                    views.overlaySeekBar.progress = percent
                }
            }
        }
    }
}
