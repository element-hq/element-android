/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.item

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.utils.TextUtils
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.home.room.detail.timeline.helper.ContentDownloadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.themes.ThemeUtils

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageAudioItem : AbsMessageItem<MessageAudioItem.Holder>() {

    @EpoxyAttribute
    var filename: String = ""

    @EpoxyAttribute
    var mxcUrl: String = ""

    @EpoxyAttribute
    var duration: Int = 0

    @EpoxyAttribute
    var fileSize: Long = 0

    @EpoxyAttribute
    @JvmField
    var isLocalFile = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onSeek: ((percentage: Float) -> Unit)? = null

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    @EpoxyAttribute
    lateinit var contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var playbackControlButtonClickListener: ClickListener? = null

    @EpoxyAttribute
    lateinit var audioMessagePlaybackTracker: AudioMessagePlaybackTracker

    private var isUserSeeking = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.rootLayout, null)
        bindViewAttributes(holder)
        bindUploadState(holder)
        applyLayoutTint(holder)
        bindSeekBar(holder)
        holder.audioPlaybackControlButton.setOnClickListener { playbackControlButtonClickListener?.invoke(it) }
        renderStateBasedOnAudioPlayback(holder)
    }

    private fun bindUploadState(holder: Holder) {
        if (attributes.informationData.sendState.hasFailed()) {
            holder.audioPlaybackControlButton.setImageResource(R.drawable.ic_cross)
            holder.audioPlaybackControlButton.contentDescription =
                    holder.view.context.getString(R.string.error_audio_message_unable_to_play, filename)
            holder.progressLayout.isVisible = false
        } else {
            contentUploadStateTrackerBinder.bind(attributes.informationData.eventId, isLocalFile, holder.progressLayout)
        }
    }

    private fun applyLayoutTint(holder: Holder) {
        val backgroundTint = if (attributes.informationData.messageLayout is TimelineMessageLayout.Bubble) {
            Color.TRANSPARENT
        } else {
            ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_quinary)
        }
        holder.mainLayout.backgroundTintList = ColorStateList.valueOf(backgroundTint)
    }

    private fun bindViewAttributes(holder: Holder) {
        val formattedDuration = formatPlaybackTime(duration)
        val formattedFileSize = TextUtils.formatFileSize(holder.rootLayout.context, fileSize, true)
        val durationContentDescription = getPlaybackTimeContentDescription(holder.rootLayout.context, duration)

        holder.filenameView.text = filename
        holder.filenameView.onClick(attributes.itemClickListener)
        holder.audioPlaybackDuration.text = formattedDuration
        holder.fileSize.text = holder.rootLayout.context.getString(
                R.string.audio_message_file_size, formattedFileSize
        )
        holder.mainLayout.contentDescription = holder.rootLayout.context.getString(
                R.string.a11y_audio_message_item, filename, durationContentDescription, formattedFileSize
        )
    }

    private fun bindSeekBar(holder: Holder) {
        holder.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                holder.audioPlaybackTime.text = formatPlaybackTime(
                        (duration * (progress.toFloat() / 100)).toInt()
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                val percentage = seekBar.progress.toFloat() / 100
                onSeek?.invoke(percentage)
            }
        })
    }

    private fun renderStateBasedOnAudioPlayback(holder: Holder) {
        audioMessagePlaybackTracker.track(attributes.informationData.eventId, object : AudioMessagePlaybackTracker.Listener {
            override fun onUpdate(state: AudioMessagePlaybackTracker.Listener.State) {
                when (state) {
                    is AudioMessagePlaybackTracker.Listener.State.Idle      -> renderIdleState(holder)
                    is AudioMessagePlaybackTracker.Listener.State.Playing   -> renderPlayingState(holder, state)
                    is AudioMessagePlaybackTracker.Listener.State.Paused    -> renderPausedState(holder, state)
                    is AudioMessagePlaybackTracker.Listener.State.Recording -> Unit
                }
            }
        })
    }

    private fun renderIdleState(holder: Holder) {
        holder.audioPlaybackControlButton.setImageResource(R.drawable.ic_play_pause_play)
        holder.audioPlaybackControlButton.contentDescription =
                holder.view.context.getString(R.string.a11y_play_audio_message, filename)
        holder.audioPlaybackTime.text = formatPlaybackTime(duration)
        holder.audioSeekBar.progress = 0
    }

    private fun renderPlayingState(holder: Holder, state: AudioMessagePlaybackTracker.Listener.State.Playing) {
        holder.audioPlaybackControlButton.setImageResource(R.drawable.ic_play_pause_pause)
        holder.audioPlaybackControlButton.contentDescription =
                holder.view.context.getString(R.string.a11y_pause_audio_message, filename)

        if (!isUserSeeking) {
            holder.audioPlaybackTime.text = formatPlaybackTime(state.playbackTime)
            holder.audioSeekBar.progress = (state.percentage * 100).toInt()
        }
    }

    private fun renderPausedState(holder: Holder, state: AudioMessagePlaybackTracker.Listener.State.Paused) {
        holder.audioPlaybackControlButton.setImageResource(R.drawable.ic_play_pause_play)
        holder.audioPlaybackControlButton.contentDescription =
                holder.view.context.getString(R.string.a11y_play_audio_message, filename)
        holder.audioPlaybackTime.text = formatPlaybackTime(state.playbackTime)
        holder.audioSeekBar.progress = (state.percentage * 100).toInt()
    }

    private fun formatPlaybackTime(time: Int) = DateUtils.formatElapsedTime((time / 1000).toLong())

    private fun getPlaybackTimeContentDescription(context: Context, time: Int): String {
        val formattedPlaybackTime = formatPlaybackTime(time)
        val (minutes, seconds) = formattedPlaybackTime.split(":").map { it.toIntOrNull() ?: 0 }
        return context.getString(R.string.a11y_audio_playback_duration, minutes, seconds)
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        contentDownloadStateTrackerBinder.unbind(mxcUrl)
        audioMessagePlaybackTracker.untrack(attributes.informationData.eventId)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val rootLayout by bind<ViewGroup>(R.id.messageRootLayout)
        val mainLayout by bind<ViewGroup>(R.id.messageMainInnerLayout)
        val filenameView by bind<TextView>(R.id.messageFilenameView)
        val audioPlaybackControlButton by bind<ImageButton>(R.id.audioPlaybackControlButton)
        val audioPlaybackTime by bind<TextView>(R.id.audioPlaybackTime)
        val progressLayout by bind<ViewGroup>(R.id.messageFileUploadProgressLayout)
        val fileSize by bind<TextView>(R.id.fileSize)
        val audioPlaybackDuration by bind<TextView>(R.id.audioPlaybackDuration)
        val audioSeekBar by bind<SeekBar>(R.id.audioSeekBar)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentAudioStub
    }
}
