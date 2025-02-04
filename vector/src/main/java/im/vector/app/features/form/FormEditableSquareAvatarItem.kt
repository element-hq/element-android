/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.form

import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class FormEditableSquareAvatarItem : VectorEpoxyModel<FormEditableSquareAvatarItem.Holder>(R.layout.item_editable_square_avatar) {

    @EpoxyAttribute
    var avatarRenderer: AvatarRenderer? = null

    @EpoxyAttribute
    var matrixItem: MatrixItem? = null

    @EpoxyAttribute
    var enabled: Boolean = true

    @EpoxyAttribute
    var imageUri: Uri? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var deleteListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.imageContainer.onClick(clickListener?.takeIf { enabled })
        when {
            imageUri != null -> {
                val corner = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f,
                        holder.view.resources.displayMetrics
                ).toInt()
                GlideApp.with(holder.image)
                        .load(imageUri)
                        .transform(MultiTransformation(CenterCrop(), RoundedCorners(corner)))
                        .into(holder.image)
            }
            matrixItem != null -> {
                avatarRenderer?.render(matrixItem!!, holder.image)
            }
            else -> {
                avatarRenderer?.clear(holder.image)
            }
        }
        holder.delete.isVisible = enabled && (imageUri != null || matrixItem?.avatarUrl?.isNotEmpty() == true)
        holder.delete.onClick(deleteListener?.takeIf { enabled })
    }

    override fun unbind(holder: Holder) {
        avatarRenderer?.clear(holder.image)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val imageContainer by bind<View>(R.id.itemEditableAvatarImageContainer)
        val image by bind<ImageView>(R.id.itemEditableAvatarImage)
        val delete by bind<View>(R.id.itemEditableAvatarDelete)
    }
}
