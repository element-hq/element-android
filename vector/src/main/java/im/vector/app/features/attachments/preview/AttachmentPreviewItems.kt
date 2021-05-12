/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.attachments.preview

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.platform.CheckableImageView
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

abstract class AttachmentPreviewItem<H : AttachmentPreviewItem.Holder> : VectorEpoxyModel<H>() {

    abstract val attachment: ContentAttachmentData

    override fun bind(holder: H) {
        super.bind(holder)
        if (attachment.type == ContentAttachmentData.Type.VIDEO || attachment.type == ContentAttachmentData.Type.IMAGE) {
            Glide.with(holder.view.context)
                    .asBitmap()
                    .load(attachment.queryUri)
                    .apply(RequestOptions().frame(0))
                    .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.filetype_attachment)
            holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    abstract class Holder : VectorEpoxyHolder() {
        abstract val imageView: ImageView
    }
}

@EpoxyModelClass(layout = R.layout.item_attachment_miniature_preview)
abstract class AttachmentMiniaturePreviewItem : AttachmentPreviewItem<AttachmentMiniaturePreviewItem.Holder>() {

    @EpoxyAttribute override lateinit var attachment: ContentAttachmentData

    @EpoxyAttribute
    var clickListener: View.OnClickListener? = null

    @EpoxyAttribute
    var checked: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.imageView.isChecked = checked
        holder.miniatureVideoIndicator.isVisible = attachment.type == ContentAttachmentData.Type.VIDEO
        holder.view.setOnClickListener(clickListener)
    }

    class Holder : AttachmentPreviewItem.Holder() {
        override val imageView: CheckableImageView
            get() = miniatureImageView
        private val miniatureImageView by bind<CheckableImageView>(R.id.attachmentMiniatureImageView)
        val miniatureVideoIndicator by bind<ImageView>(R.id.attachmentMiniatureVideoIndicator)
    }
}

@EpoxyModelClass(layout = R.layout.item_attachment_big_preview)
abstract class AttachmentBigPreviewItem : AttachmentPreviewItem<AttachmentBigPreviewItem.Holder>() {

    @EpoxyAttribute override lateinit var attachment: ContentAttachmentData

    class Holder : AttachmentPreviewItem.Holder() {
        override val imageView: ImageView
            get() = bigImageView
        private val bigImageView by bind<ImageView>(R.id.attachmentBigImageView)
    }
}
