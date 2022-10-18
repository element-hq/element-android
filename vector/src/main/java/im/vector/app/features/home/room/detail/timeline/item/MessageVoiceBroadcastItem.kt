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

import android.annotation.SuppressLint
import android.widget.ImageButton
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.room.detail.RoomDetailAction.VoiceBroadcastAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker.Listener.State
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState

@EpoxyModelClass
abstract class MessageVoiceBroadcastItem : AbsMessageItem<MessageVoiceBroadcastItem.Holder>() {

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var voiceBroadcastState: VoiceBroadcastState? = null

    @EpoxyAttribute
    var recording: Boolean = false

    @EpoxyAttribute
    lateinit var audioMessagePlaybackTracker: AudioMessagePlaybackTracker

    private val voiceBroadcastEventId
        get() = attributes.informationData.eventId

    override fun isCacheable(): Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    @SuppressLint("SetTextI18n") // Temporary text
    private fun bindVoiceBroadcastItem(holder: Holder) {
        holder.currentStateText.text = "Voice Broadcast state: ${voiceBroadcastState?.value ?: "None"}"
        if (recording) {
            renderRecording(holder)
        } else {
            renderListening(holder)
        }
    }

    private fun renderListening(holder: Holder) {
        audioMessagePlaybackTracker.track(attributes.informationData.eventId, object : AudioMessagePlaybackTracker.Listener {
            override fun onUpdate(state: State) {
                holder.playButton.isEnabled = state !is State.Playing
                holder.pauseButton.isEnabled = state is State.Playing
                holder.stopButton.isEnabled = state !is State.Idle
            }
        })
        holder.playButton.setOnClickListener { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcastEventId)) }
        holder.pauseButton.setOnClickListener { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.Pause) }
        holder.stopButton.setOnClickListener { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.Stop) }
    }

    private fun renderRecording(holder: Holder) {
        with(holder) {
            playButton.isEnabled = voiceBroadcastState == VoiceBroadcastState.PAUSED
            pauseButton.isEnabled = voiceBroadcastState == VoiceBroadcastState.STARTED || voiceBroadcastState == VoiceBroadcastState.RESUMED
            stopButton.isEnabled = voiceBroadcastState == VoiceBroadcastState.STARTED ||
                    voiceBroadcastState == VoiceBroadcastState.RESUMED ||
                    voiceBroadcastState == VoiceBroadcastState.PAUSED
            playButton.setOnClickListener { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Resume) }
            pauseButton.setOnClickListener { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Pause) }
            stopButton.setOnClickListener { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Stop) }
        }
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageLocationItem.Holder(STUB_ID) {
        val currentStateText by bind<TextView>(R.id.currentStateText)
        val playButton by bind<ImageButton>(R.id.playButton)
        val pauseButton by bind<ImageButton>(R.id.pauseButton)
        val stopButton by bind<ImageButton>(R.id.stopButton)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastStub
    }
}
