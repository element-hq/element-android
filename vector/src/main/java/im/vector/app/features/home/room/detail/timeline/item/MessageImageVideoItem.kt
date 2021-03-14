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

package im.vector.app.features.home.room.detail.timeline.item

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.media.ImageContentRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute
    lateinit var mediaData: ImageContentRenderer.Data

    @EpoxyAttribute
    var playable: Boolean = false

    @EpoxyAttribute
    var mode = ImageContentRenderer.Mode.THUMBNAIL

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: View.OnClickListener? = null

    @EpoxyAttribute
    lateinit var imageContentRenderer: ImageContentRenderer

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    override fun bind(holder: Holder) {
        super.bind(holder)
        imageContentRenderer.render(mediaData, mode, holder.imageView)
        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(
                    attributes.informationData.eventId,
                    LocalFilesHelper(holder.view.context).isLocalFile(mediaData.url),
                    holder.progressLayout
            )
        } else {
            holder.progressLayout.isVisible = false
        }
        holder.imageView.setOnClickListener(clickListener)
        holder.imageView.setOnLongClickListener(attributes.itemLongClickListener)
        ViewCompat.setTransitionName(holder.imageView, "imagePreview_${id()}")
        holder.mediaContentView.setOnClickListener(attributes.itemClickListener)
        holder.mediaContentView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.playContentView.visibility = if (playable) View.VISIBLE else View.GONE
    }

    override fun unbind(holder: Holder) {
        GlideApp.with(holder.view.context.applicationContext).clear(holder.imageView)
        imageContentRenderer.clear(holder.imageView)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        holder.imageView.setOnClickListener(null)
        holder.imageView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    override fun getViewType() = STUB_ID

    override fun messageBubbleAllowed(context: Context): Boolean {
        return false
    }

    override fun pseudoBubbleAllowed(): Boolean {
        return true
    }

    override fun getBubbleMargin(resources: Resources, bubbleStyle: String, reverseBubble: Boolean): Int {
        return 0
    }

    override fun setBubbleLayout(holder: Holder, bubbleStyle: String, bubbleStyleSetting: String, reverseBubble: Boolean) {
        super.setBubbleLayout(holder, bubbleStyle, bubbleStyleSetting, reverseBubble)

        // Case: ImageContentRenderer.processSize only sees width=height=0 -> width of the ImageView not adapted to the actual content
        // -> Align image within ImageView to same side as message bubbles
        holder.imageView.scaleType = if (reverseBubble) ImageView.ScaleType.FIT_END else ImageView.ScaleType.FIT_START
        // Case: Message information (sender name + date) makes the containing view wider than the ImageView
        // -> Align ImageView within its parent to the same side as message bubbles
        (holder.imageView.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = if (reverseBubble) 1f else 0f
    }

    override fun allowFooterOverlay(holder: Holder): Boolean {
        return true
    }



    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)
        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)
        val mediaContentView by bind<ViewGroup>(R.id.messageContentMedia)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMediaStub
    }
}
