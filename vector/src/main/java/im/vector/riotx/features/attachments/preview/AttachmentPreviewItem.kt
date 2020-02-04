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
 *
 */

package im.vector.riotx.features.attachments.preview

import android.view.View
import android.widget.ImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import kotlinx.android.synthetic.main.item_attachment_preview.view.*

@EpoxyModelClass(layout = R.layout.item_attachment_preview)
abstract class AttachmentPreviewItem : VectorEpoxyModel<AttachmentPreviewItem.Holder>() {

    @EpoxyAttribute lateinit var attachment: ContentAttachmentData
    @EpoxyAttribute var clickListener: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        holder.view.setOnClickListener(clickListener)
        // If name is empty, use userId as name and force it being centered
        val mimeType = attachment.mimeType
        val path = attachment.path
        if (mimeType != null && (mimeType.startsWith("image") || mimeType.startsWith("video"))) {
            Glide.with(holder.view.context)
                    .asBitmap()
                    .load(path)
                    .apply(RequestOptions().frame(0))
                    .into(holder.imageView)
        } else {
            holder.imageView.attachmentPreviewImageView.setImageResource(R.drawable.filetype_attachment)
            holder.imageView.attachmentPreviewImageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    class Holder : VectorEpoxyHolder() {
        val imageView by bind<ImageView>(R.id.attachmentPreviewImageView)
    }
}
