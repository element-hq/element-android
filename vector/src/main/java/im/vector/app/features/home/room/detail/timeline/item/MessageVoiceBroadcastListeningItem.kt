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
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.voicebroadcast.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.views.VoiceBroadcastMetadataView

@EpoxyModelClass
abstract class MessageVoiceBroadcastListeningItem : AbsMessageVoiceBroadcastItem<MessageVoiceBroadcastListeningItem.Holder>() {

    private lateinit var playerListener: VoiceBroadcastPlayer.Listener

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    private fun bindVoiceBroadcastItem(holder: Holder) {
        playerListener = VoiceBroadcastPlayer.Listener { state ->
            renderPlayingState(holder, state)
        }
        player.addListener(voiceBroadcastId, playerListener)
    }

    override fun renderMetadata(holder: Holder) {
        with(holder) {
            broadcasterNameMetadata.value = recorderName
            voiceBroadcastMetadata.isVisible = true
            listenersCountMetadata.isVisible = false
        }
    }

    private fun renderPlayingState(holder: Holder, state: VoiceBroadcastPlayer.State) {
        with(holder) {
            bufferingView.isVisible = state == VoiceBroadcastPlayer.State.BUFFERING
            playPauseButton.isVisible = state != VoiceBroadcastPlayer.State.BUFFERING

            when (state) {
                VoiceBroadcastPlayer.State.PLAYING -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_pause)
                    playPauseButton.contentDescription = view.resources.getString(R.string.a11y_play_voice_broadcast)
                    playPauseButton.onClick { callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Listening.Pause) }
                }
                VoiceBroadcastPlayer.State.IDLE,
                VoiceBroadcastPlayer.State.PAUSED -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_play)
                    playPauseButton.contentDescription = view.resources.getString(R.string.a11y_pause_voice_broadcast)
                    playPauseButton.onClick {
                        callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcastId))
                    }
                }
                VoiceBroadcastPlayer.State.BUFFERING -> Unit
            }
        }
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        player.removeListener(voiceBroadcastId, playerListener)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageVoiceBroadcastItem.Holder(STUB_ID) {
        val playPauseButton by bind<ImageButton>(R.id.playPauseButton)
        val bufferingView by bind<View>(R.id.bufferingView)
        val broadcasterNameMetadata by bind<VoiceBroadcastMetadataView>(R.id.broadcasterNameMetadata)
        val voiceBroadcastMetadata by bind<VoiceBroadcastMetadataView>(R.id.voiceBroadcastMetadata)
        val listenersCountMetadata by bind<VoiceBroadcastMetadataView>(R.id.listenersCountMetadata)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastListeningStub
    }
}