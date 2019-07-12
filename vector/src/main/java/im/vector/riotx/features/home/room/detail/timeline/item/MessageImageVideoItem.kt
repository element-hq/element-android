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

package im.vector.riotx.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.riotx.features.media.ImageContentRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute
    lateinit var mediaData: ImageContentRenderer.Data
    @EpoxyAttribute
    var playable: Boolean = false
    @EpoxyAttribute
    var clickListener: View.OnClickListener? = null
    @EpoxyAttribute
    lateinit var imageContentRenderer: ImageContentRenderer
    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    override fun bind(holder: Holder) {
        super.bind(holder)
        imageContentRenderer.render(mediaData, ImageContentRenderer.Mode.THUMBNAIL, holder.imageView)
        contentUploadStateTrackerBinder.bind(informationData.eventId, mediaData, holder.progressLayout)
        holder.imageView.setOnClickListener(clickListener)
        holder.imageView.setOnLongClickListener(longClickListener)
        holder.mediaContentView.setOnClickListener(cellClickListener)
        holder.mediaContentView.setOnLongClickListener(longClickListener)
        // The sending state color will be apply to the progress text
        renderSendState(holder.imageView, null)
        holder.playContentView.visibility = if (playable) View.VISIBLE else View.GONE
    }

    override fun unbind(holder: Holder) {
        contentUploadStateTrackerBinder.unbind(informationData.eventId)
        super.unbind(holder)
    }

    override fun getStubType(): Int = STUB_ID

    class Holder : AbsMessageItem.Holder() {

        override fun getStubId(): Int = STUB_ID

        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)
        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)

        val mediaContentView by bind<ViewGroup>(R.id.messageContentMedia)

    }


    companion object {
        private val STUB_ID = R.id.messageContentMediaStub
    }

}