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

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.tintBackground
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.voicebroadcast.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class MessageVoiceBroadcastListeningItem : AbsMessageItem<MessageVoiceBroadcastListeningItem.Holder>() {

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var voiceBroadcastPlayer: VoiceBroadcastPlayer? = null

    @EpoxyAttribute
    lateinit var voiceBroadcastId: String

    @EpoxyAttribute
    var voiceBroadcastState: VoiceBroadcastState? = null

    @EpoxyAttribute
    var broadcasterName: String? = null

    @EpoxyAttribute
    lateinit var colorProvider: ColorProvider

    @EpoxyAttribute
    lateinit var drawableProvider: DrawableProvider

    @EpoxyAttribute
    var roomItem: MatrixItem? = null

    @EpoxyAttribute
    var title: String? = null

    private lateinit var playerListener: VoiceBroadcastPlayer.Listener

    override fun isCacheable(): Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    private fun bindVoiceBroadcastItem(holder: Holder) {
        playerListener = VoiceBroadcastPlayer.Listener { state ->
            renderState(holder, state)
        }
        voiceBroadcastPlayer?.addListener(playerListener)
        renderHeader(holder)
        renderLiveIcon(holder)
    }

    private fun renderHeader(holder: Holder) {
        with(holder) {
            roomItem?.let {
                attributes.avatarRenderer.render(it, roomAvatarImageView)
                titleText.text = it.displayName
            }
            broadcasterNameText.text = broadcasterName
        }
    }

    private fun renderLiveIcon(holder: Holder) {
        with(holder) {
            when (voiceBroadcastState) {
                VoiceBroadcastState.STARTED,
                VoiceBroadcastState.RESUMED -> {
                    liveIndicator.tintBackground(colorProvider.getColorFromAttribute(R.attr.colorError))
                    liveIndicator.isVisible = true
                }
                VoiceBroadcastState.PAUSED -> {
                    liveIndicator.tintBackground(colorProvider.getColorFromAttribute(R.attr.vctr_content_quaternary))
                    liveIndicator.isVisible = true
                }
                VoiceBroadcastState.STOPPED, null -> {
                    liveIndicator.isVisible = false
                }
            }
        }
    }

    private fun renderState(holder: Holder, state: VoiceBroadcastPlayer.State) {
        if (isCurrentMediaActive()) {
            renderActiveMedia(holder, state)
        } else {
            renderInactiveMedia(holder)
        }
    }

    private fun renderActiveMedia(holder: Holder, state: VoiceBroadcastPlayer.State) {
        with(holder) {
            bufferingView.isVisible = state == VoiceBroadcastPlayer.State.BUFFERING
            playPauseButton.isVisible = state != VoiceBroadcastPlayer.State.BUFFERING

            when (state) {
                VoiceBroadcastPlayer.State.PLAYING -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_pause)
                    playPauseButton.contentDescription = view.resources.getString(R.string.a11y_play_voice_broadcast)
                    playPauseButton.onClick { attributes.callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Listening.Pause) }
                }
                VoiceBroadcastPlayer.State.IDLE,
                VoiceBroadcastPlayer.State.PAUSED -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_play)
                    playPauseButton.contentDescription = view.resources.getString(R.string.a11y_pause_voice_broadcast)
                    playPauseButton.onClick {
                        attributes.callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcastId))
                    }
                }
                VoiceBroadcastPlayer.State.BUFFERING -> Unit
            }
        }
    }

    private fun renderInactiveMedia(holder: Holder) {
        with(holder) {
            bufferingView.isVisible = false
            playPauseButton.isVisible = true
            playPauseButton.setImageResource(R.drawable.ic_play_pause_play)
            playPauseButton.contentDescription = view.resources.getString(R.string.a11y_pause_voice_broadcast)
            playPauseButton.onClick {
                attributes.callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcastId))
            }
        }
    }

    private fun isCurrentMediaActive() = voiceBroadcastPlayer?.currentVoiceBroadcastId == voiceBroadcastId

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        voiceBroadcastPlayer?.removeListener(playerListener)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val liveIndicator by bind<TextView>(R.id.liveIndicator)
        val roomAvatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val titleText by bind<TextView>(R.id.titleText)
        val playPauseButton by bind<ImageButton>(R.id.playPauseButton)
        val bufferingView by bind<View>(R.id.bufferingView)
        val broadcasterNameText by bind<TextView>(R.id.broadcasterNameText)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastListeningStub
    }
}
