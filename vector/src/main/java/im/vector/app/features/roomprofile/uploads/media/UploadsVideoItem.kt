/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads.media

import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer

@EpoxyModelClass
abstract class UploadsVideoItem : VectorEpoxyModel<UploadsVideoItem.Holder>(R.layout.item_uploads_video) {

    @EpoxyAttribute lateinit var imageContentRenderer: ImageContentRenderer
    @EpoxyAttribute lateinit var data: VideoContentRenderer.Data
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.onClick(listener)
        imageContentRenderer.render(data.thumbnailMediaData, holder.imageView, IMAGE_SIZE_DP)
        ViewCompat.setTransitionName(holder.imageView, "videoPreview_${id()}")
    }

    class Holder : VectorEpoxyHolder() {
        val imageView by bind<ImageView>(R.id.uploadsVideoPreview)
    }
}
