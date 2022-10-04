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
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState

@EpoxyModelClass
abstract class MessageVoiceBroadcastItem : AbsMessageItem<MessageVoiceBroadcastItem.Holder>() {

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var voiceBroadcastState: VoiceBroadcastState? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    @SuppressLint("SetTextI18n") // Temporary text
    private fun bindVoiceBroadcastItem(holder: Holder) {
        with(holder) {
            currentStateText.text = "Voice Broadcast state: ${voiceBroadcastState?.value ?: "None"}"
            playButton.isEnabled = voiceBroadcastState == VoiceBroadcastState.PAUSED
            pauseButton.isEnabled = voiceBroadcastState == VoiceBroadcastState.STARTED || voiceBroadcastState == VoiceBroadcastState.RESUMED
            stopButton.isEnabled = voiceBroadcastState == VoiceBroadcastState.STARTED ||
                    voiceBroadcastState == VoiceBroadcastState.RESUMED ||
                    voiceBroadcastState == VoiceBroadcastState.PAUSED
            playButton.setOnClickListener { attributes.callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Resume) }
            pauseButton.setOnClickListener { attributes.callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Pause) }
            stopButton.setOnClickListener { attributes.callback?.onTimelineItemAction(RoomDetailAction.VoiceBroadcastAction.Stop) }
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
