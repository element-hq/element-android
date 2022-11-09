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

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.app.R
import im.vector.app.core.extensions.tintBackground
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import org.matrix.android.sdk.api.util.MatrixItem

abstract class AbsMessageVoiceBroadcastItem<H : AbsMessageVoiceBroadcastItem.Holder> : AbsMessageItem<H>() {

    @EpoxyAttribute
    lateinit var voiceBroadcastAttributes: Attributes

    protected val voiceBroadcastId get() = voiceBroadcastAttributes.voiceBroadcastId
    protected val voiceBroadcastState get() = voiceBroadcastAttributes.voiceBroadcastState
    protected val recorderName get() = voiceBroadcastAttributes.recorderName
    protected val recorder get() = voiceBroadcastAttributes.recorder
    protected val player get() = voiceBroadcastAttributes.player
    protected val roomItem get() = voiceBroadcastAttributes.roomItem
    protected val colorProvider get() = voiceBroadcastAttributes.colorProvider
    protected val drawableProvider get() = voiceBroadcastAttributes.drawableProvider
    protected val avatarRenderer get() = attributes.avatarRenderer
    protected val callback get() = attributes.callback

    override fun isCacheable(): Boolean = false

    override fun bind(holder: H) {
        super.bind(holder)
        renderHeader(holder)
    }

    private fun renderHeader(holder: H) {
        with(holder) {
            roomItem?.let {
                avatarRenderer.render(it, roomAvatarImageView)
                titleText.text = it.displayName
            }
        }
        renderLiveIndicator(holder)
        renderMetadata(holder)
    }

    private fun renderLiveIndicator(holder: H) {
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

    abstract fun renderMetadata(holder: H)

    abstract class Holder(@IdRes stubId: Int) : AbsMessageItem.Holder(stubId) {
        val liveIndicator by bind<TextView>(R.id.liveIndicator)
        val roomAvatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val titleText by bind<TextView>(R.id.titleText)
    }

    data class Attributes(
            val voiceBroadcastId: String,
            val voiceBroadcastState: VoiceBroadcastState?,
            val duration: Int,
            val recorderName: String,
            val recorder: VoiceBroadcastRecorder?,
            val player: VoiceBroadcastPlayer,
            val roomItem: MatrixItem?,
            val colorProvider: ColorProvider,
            val drawableProvider: DrawableProvider,
    )
}
