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
import im.vector.app.features.home.room.detail.RoomDetailAction.VoiceBroadcastAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.voicebroadcast.VoiceBroadcastRecorder
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class MessageVoiceBroadcastRecordingItem : AbsMessageItem<MessageVoiceBroadcastRecordingItem.Holder>() {

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var voiceBroadcastRecorder: VoiceBroadcastRecorder? = null

    @EpoxyAttribute
    lateinit var colorProvider: ColorProvider

    @EpoxyAttribute
    lateinit var drawableProvider: DrawableProvider

    @EpoxyAttribute
    var roomItem: MatrixItem? = null

    @EpoxyAttribute
    var title: String? = null

    private lateinit var recorderListener: VoiceBroadcastRecorder.Listener

    override fun isCacheable(): Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    private fun bindVoiceBroadcastItem(holder: Holder) {
        recorderListener = object : VoiceBroadcastRecorder.Listener {
            override fun onStateUpdated(state: VoiceBroadcastRecorder.State) {
                renderState(holder, state)
            }
        }
        voiceBroadcastRecorder?.addListener(recorderListener)
        renderHeader(holder)
    }

    private fun renderHeader(holder: Holder) {
        with(holder) {
            roomItem?.let {
                attributes.avatarRenderer.render(it, roomAvatarImageView)
                titleText.text = it.displayName
            }
        }
    }

    private fun renderState(holder: Holder, state: VoiceBroadcastRecorder.State) {
        with(holder) {
            when (state) {
                VoiceBroadcastRecorder.State.Recording -> {
                    stopRecordButton.isEnabled = true
                    recordButton.isEnabled = true

                    liveIndicator.isVisible = true
                    liveIndicator.tintBackground(colorProvider.getColorFromAttribute(R.attr.colorError))

                    val drawableColor = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                    val drawable = drawableProvider.getDrawable(R.drawable.ic_play_pause_pause, drawableColor)
                    recordButton.setImageDrawable(drawable)
                    recordButton.contentDescription = holder.view.resources.getString(R.string.a11y_pause_voice_broadcast_record)
                    recordButton.onClick { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Pause) }
                    stopRecordButton.onClick { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Stop) }
                }
                VoiceBroadcastRecorder.State.Paused -> {
                    stopRecordButton.isEnabled = true
                    recordButton.isEnabled = true

                    liveIndicator.isVisible = true
                    liveIndicator.tintBackground(colorProvider.getColorFromAttribute(R.attr.vctr_content_quaternary))

                    recordButton.setImageResource(R.drawable.ic_recording_dot)
                    recordButton.contentDescription = holder.view.resources.getString(R.string.a11y_resume_voice_broadcast_record)
                    recordButton.onClick { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Resume) }
                    stopRecordButton.onClick { attributes.callback?.onTimelineItemAction(VoiceBroadcastAction.Recording.Stop) }
                }
                VoiceBroadcastRecorder.State.Idle -> {
                    recordButton.isEnabled = false
                    stopRecordButton.isEnabled = false
                    liveIndicator.isVisible = false
                }
            }
        }
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        voiceBroadcastRecorder?.removeListener(recorderListener)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val liveIndicator by bind<TextView>(R.id.liveIndicator)
        val roomAvatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val titleText by bind<TextView>(R.id.titleText)
        val recordButton by bind<ImageButton>(R.id.recordButton)
        val stopRecordButton by bind<ImageButton>(R.id.stopRecordButton)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastRecordingStub
    }
}
