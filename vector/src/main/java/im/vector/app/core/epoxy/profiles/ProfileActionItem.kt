/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy.profiles

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class ProfileActionItem : VectorEpoxyModel<ProfileActionItem.Holder>(R.layout.item_profile_action) {

    @EpoxyAttribute
    lateinit var title: String

    @EpoxyAttribute
    var subtitle: String? = null

    @EpoxyAttribute
    var iconRes: Int = 0

    @EpoxyAttribute
    var tintIcon: Boolean = true

    @EpoxyAttribute
    var editableRes: Int = R.drawable.ic_arrow_right

    @EpoxyAttribute
    var accessoryRes: Int = 0

    @EpoxyAttribute
    var accessoryMatrixItem: MatrixItem? = null

    @EpoxyAttribute
    var avatarRenderer: AvatarRenderer? = null

    @EpoxyAttribute
    var editable: Boolean = true

    @EpoxyAttribute
    var destructive: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.onClick(listener)
        if (listener == null) {
            holder.view.isClickable = false
        }
        holder.title.text = title
        val titleTintColor = if (destructive) {
            ThemeUtils.getColor(holder.view.context, com.google.android.material.R.attr.colorError)
        } else {
            ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_primary)
        }
        val iconTintColor = if (destructive) {
            ThemeUtils.getColor(holder.view.context, com.google.android.material.R.attr.colorError)
        } else {
            ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        }
        holder.title.setTextColor(titleTintColor)
        holder.subtitle.setTextOrHide(subtitle)
        if (iconRes != 0) {
            holder.icon.setImageResource(iconRes)
            if (tintIcon) {
                ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(iconTintColor))
            } else {
                ImageViewCompat.setImageTintList(holder.icon, null)
            }
            holder.icon.isVisible = true
        } else {
            holder.icon.isVisible = false
        }

        if (accessoryRes != 0) {
            holder.secondaryAccessory.setImageResource(accessoryRes)
            holder.secondaryAccessory.isVisible = true
        } else {
            holder.secondaryAccessory.isVisible = false
        }

        if (accessoryMatrixItem != null) {
            avatarRenderer?.render(accessoryMatrixItem!!, holder.secondaryAccessory)
            holder.secondaryAccessory.isVisible = true
        } else {
            holder.secondaryAccessory.isVisible = false
        }

        if (editableRes != 0 && editable) {
            val tintColorSecondary = if (destructive) {
                titleTintColor
            } else {
                ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
            }
            holder.editable.setImageResource(editableRes)
            ImageViewCompat.setImageTintList(holder.editable, ColorStateList.valueOf(tintColorSecondary))
            holder.editable.isVisible = true
        } else {
            holder.editable.isVisible = false
        }
    }

    class Holder : VectorEpoxyHolder() {
        val icon by bind<ImageView>(R.id.actionIcon)
        val title by bind<TextView>(R.id.actionTitle)
        val subtitle by bind<TextView>(R.id.actionSubtitle)
        val editable by bind<ImageView>(R.id.actionEditable)
        val secondaryAccessory by bind<ImageView>(R.id.actionSecondaryAccessory)
    }
}
