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
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.themes.BubbleThemeUtils
import org.matrix.android.sdk.api.util.MimeTypes

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute
    lateinit var mediaData: ImageContentRenderer.Data

    @EpoxyAttribute
    var playable: Boolean = false

    @EpoxyAttribute
    var mode = ImageContentRenderer.Mode.THUMBNAIL

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    @EpoxyAttribute
    lateinit var imageContentRenderer: ImageContentRenderer

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    var lastAllowedFooterOverlay: Boolean = true
    var lastShowFooterBellow: Boolean = true
    var forceAllowFooterOverlay: Boolean? = null
    var showFooterBellow: Boolean = true

    override fun bind(holder: Holder) {
        forceAllowFooterOverlay = null
        super.bind(holder)

        val onImageSizeListener = object: ImageContentRenderer.OnImageSizeListener {
            override fun onImageSizeUpdated(width: Int, height: Int) {
                // Image size change -> different footer space situation possible
                val footerMeasures = getFooterMeasures(holder)
                forceAllowFooterOverlay = shouldAllowFooterOverlay(footerMeasures, width, height)
                val newShowFooterBellow = shouldShowFooterBellow(footerMeasures, width, height)
                if (lastAllowedFooterOverlay != forceAllowFooterOverlay || newShowFooterBellow != lastShowFooterBellow) {
                    showFooterBellow = newShowFooterBellow
                    updateMessageBubble(holder.imageView.context, holder)
                }
            }
        }
        val animate = mediaData.mimeType == MimeTypes.Gif
        // Do not use thumbnails for animated GIFs - sometimes thumbnails do not animate while the original GIF does
        val effectiveMode = if (animate && mode == ImageContentRenderer.Mode.THUMBNAIL) ImageContentRenderer.Mode.ANIMATED_THUMBNAIL else mode
        imageContentRenderer.render(mediaData, effectiveMode, holder.imageView, onImageSizeListener, animate)
        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(
                    attributes.informationData.eventId,
                    LocalFilesHelper(holder.view.context).isLocalFile(mediaData.url),
                    holder.progressLayout
            )
        } else {
            holder.progressLayout.isVisible = false
        }
        holder.imageView.onClick(clickListener)
        holder.imageView.setOnLongClickListener(attributes.itemLongClickListener)
        ViewCompat.setTransitionName(holder.imageView, "imagePreview_${id()}")
        holder.mediaContentView.onClick(attributes.itemClickListener)
        holder.mediaContentView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.playContentView.visibility = if (playable && !animate) View.VISIBLE else View.GONE
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

        // Image outline
        when {
            bubbleStyle == BubbleThemeUtils.BUBBLE_STYLE_NONE || mode != ImageContentRenderer.Mode.THUMBNAIL -> {
                // Don't show it for non-bubble layouts, don't show for Stickers, ...
                holder.mediaContentView.background = null
            }
            attributes.informationData.sentByMe                                                           -> {
                holder.mediaContentView.setBackgroundResource(R.drawable.background_image_border_outgoing)
            }
            else                                        -> {
                holder.mediaContentView.setBackgroundResource(R.drawable.background_image_border_incoming)
            }
        }
    }

    private fun shouldAllowFooterOverlay(footerMeasures: Array<Int>, imageWidth: Int, imageHeight: Int): Boolean {
        val footerWidth = footerMeasures[0]
        val footerHeight = footerMeasures[1]
        // We need enough space in both directions to remain within the image bounds.
        // Furthermore, we do not want to hide a too big area, so check the total covered area as well.
        return imageWidth > 1.5*footerWidth && imageHeight > 1.5*footerHeight && (imageWidth * imageHeight > 4 * footerWidth * footerHeight)
    }

    private fun shouldShowFooterBellow(footerMeasures: Array<Int>, imageWidth: Int, imageHeight: Int): Boolean {
        // Only show footer bellow if the width is not the limiting factor (or it will get cut).
        // Otherwise, we can not be sure in this place that we'll have enough space on the side
        // Also, prefer footer on the side if possible (i.e. enough height available)
        val footerWidth = footerMeasures[0]
        val footerHeight = footerMeasures[1]
        return imageWidth > 1.5*footerWidth && imageHeight < 1.5*footerHeight
    }

    override fun allowFooterOverlay(holder: Holder): Boolean {
        val rememberedAllowFooterOverlay = forceAllowFooterOverlay
        if (rememberedAllowFooterOverlay != null) {
            lastAllowedFooterOverlay = rememberedAllowFooterOverlay
            return rememberedAllowFooterOverlay
        }
        val imageWidth = holder.imageView.width
        val imageHeight = holder.imageView.height
        if (imageWidth == 0 && imageHeight == 0) {
            // Not initialised yet, assume true
            lastAllowedFooterOverlay = true
            return true
        }
        // If the footer covers most of the image, or is even larger than the image, move it outside
        val footerMeasures = getFooterMeasures(holder)
        lastAllowedFooterOverlay = shouldAllowFooterOverlay(footerMeasures, imageWidth, imageHeight)
        return lastAllowedFooterOverlay
    }

    override fun allowFooterBelow(holder: Holder): Boolean {
        val showBellow = showFooterBellow
        lastShowFooterBellow = showBellow
        return showBellow
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
