/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.riotredesign.features.media.ImageContentRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_image_video_message)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute lateinit var mediaData: ImageContentRenderer.Data
    @EpoxyAttribute override lateinit var informationData: MessageInformationData
    @EpoxyAttribute var playable: Boolean = false
    @EpoxyAttribute var clickListener: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        ImageContentRenderer.render(mediaData, ImageContentRenderer.Mode.THUMBNAIL, holder.imageView)
        ContentUploadStateTrackerBinder.bind(informationData.eventId, mediaData, holder.progressLayout)
        holder.imageView.setOnClickListener(clickListener)
        holder.imageView.renderSendState()
        holder.playContentView.visibility = if (playable) View.VISIBLE else View.GONE
    }

    override fun unbind(holder: Holder) {
        ContentUploadStateTrackerBinder.unbind(informationData.eventId)
        super.unbind(holder)
    }

    class Holder : AbsMessageItem.Holder() {
        override val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        override val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        override val timeView by bind<TextView>(R.id.messageTimeView)
        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)
        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)
    }

}