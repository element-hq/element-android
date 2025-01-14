/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.utils.TextUtils
import im.vector.app.features.home.room.detail.RoomDetailAction.VoiceBroadcastAction
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import im.vector.app.features.voicebroadcast.views.VoiceBroadcastMetadataView
import im.vector.lib.strings.CommonStrings
import org.threeten.bp.Duration

@EpoxyModelClass
abstract class MessageVoiceBroadcastRecordingItem : AbsMessageVoiceBroadcastItem<MessageVoiceBroadcastRecordingItem.Holder>() {

    private var recorderListener: VoiceBroadcastRecorder.Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    private fun bindVoiceBroadcastItem(holder: Holder) {
        if (recorder != null && recorder?.recordingState != VoiceBroadcastRecorder.State.Idle) {
            recorderListener = object : VoiceBroadcastRecorder.Listener {
                override fun onStateUpdated(state: VoiceBroadcastRecorder.State) {
                    renderRecordingState(holder, state)
                }

                override fun onRemainingTimeUpdated(remainingTime: Long?) {
                    renderRemainingTime(holder, remainingTime)
                }
            }.also { recorder?.addListener(it) }
        } else {
            renderVoiceBroadcastState(holder)
        }
    }

    override fun renderLiveIndicator(holder: Holder) {
        when (recorder?.recordingState) {
            VoiceBroadcastRecorder.State.Recording -> renderPlayingLiveIndicator(holder)
            VoiceBroadcastRecorder.State.Error,
            VoiceBroadcastRecorder.State.Paused -> renderPausedLiveIndicator(holder)
            VoiceBroadcastRecorder.State.Idle, null -> renderNoLiveIndicator(holder)
        }
    }

    override fun renderMetadata(holder: Holder) {
        holder.listenersCountMetadata.isVisible = false
    }

    private fun renderRemainingTime(holder: Holder, remainingTime: Long?) {
        if (remainingTime != null) {
            val formattedDuration = TextUtils.formatDurationWithUnits(
                    holder.view.context,
                    Duration.ofSeconds(remainingTime.coerceAtLeast(0L))
            )
            holder.remainingTimeMetadata.value = holder.view.resources.getString(CommonStrings.voice_broadcast_recording_time_left, formattedDuration)
            holder.remainingTimeMetadata.isVisible = true
        } else {
            holder.remainingTimeMetadata.isVisible = false
        }
    }

    private fun renderRecordingState(holder: Holder, state: VoiceBroadcastRecorder.State) {
        when (state) {
            VoiceBroadcastRecorder.State.Recording -> renderRecordingState(holder)
            VoiceBroadcastRecorder.State.Paused -> renderPausedState(holder)
            VoiceBroadcastRecorder.State.Idle -> renderStoppedState(holder)
            VoiceBroadcastRecorder.State.Error -> renderErrorState(holder, true)
        }
        renderLiveIndicator(holder)
    }

    private fun renderVoiceBroadcastState(holder: Holder) {
        when (voiceBroadcastState) {
            VoiceBroadcastState.STARTED,
            VoiceBroadcastState.RESUMED -> renderRecordingState(holder)
            VoiceBroadcastState.PAUSED -> renderPausedState(holder)
            VoiceBroadcastState.STOPPED,
            null -> renderStoppedState(holder)
        }
    }

    private fun renderRecordingState(holder: Holder) = with(holder) {
        stopRecordButton.isEnabled = true
        recordButton.isEnabled = true
        renderErrorState(holder, false)

        val drawableColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        val drawable = drawableProvider.getDrawable(R.drawable.ic_play_pause_pause, drawableColor)
        recordButton.setImageDrawable(drawable)
        recordButton.contentDescription = holder.view.resources.getString(CommonStrings.a11y_pause_voice_broadcast_record)
        recordButton.onClick { callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Pause) }
        stopRecordButton.onClick { callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Stop) }
    }

    private fun renderPausedState(holder: Holder) = with(holder) {
        stopRecordButton.isEnabled = true
        recordButton.isEnabled = true
        renderErrorState(holder, false)

        recordButton.setImageResource(R.drawable.ic_recording_dot)
        recordButton.contentDescription = holder.view.resources.getString(CommonStrings.a11y_resume_voice_broadcast_record)
        recordButton.onClick { callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Resume) }
        stopRecordButton.onClick { callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Stop) }
    }

    private fun renderStoppedState(holder: Holder) = with(holder) {
        recordButton.isEnabled = false
        stopRecordButton.isEnabled = false
        renderErrorState(holder, false)
    }

    private fun renderErrorState(holder: Holder, isOnError: Boolean) = with(holder) {
        controlsGroup.isVisible = !isOnError
        errorView.isVisible = isOnError
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        recorderListener?.let { recorder?.removeListener(it) }
        recorderListener = null
        with(holder) {
            recordButton.onClick(null)
            stopRecordButton.onClick(null)
        }
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageVoiceBroadcastItem.Holder(STUB_ID) {
        val listenersCountMetadata by bind<VoiceBroadcastMetadataView>(R.id.listenersCountMetadata)
        val remainingTimeMetadata by bind<VoiceBroadcastMetadataView>(R.id.remainingTimeMetadata)
        val recordButton by bind<ImageButton>(R.id.recordButton)
        val stopRecordButton by bind<ImageButton>(R.id.stopRecordButton)
        val errorView by bind<TextView>(R.id.errorView)
        val controlsGroup by bind<Group>(R.id.controlsGroup)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastRecordingStub
    }
}
