/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.epoxy.bottomsheet

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.MatrixItem

/**
 * A room preview for bottom sheet.
 */
@EpoxyModelClass
abstract class BottomSheetRoomPreviewItem : VectorEpoxyModel<BottomSheetRoomPreviewItem.Holder>(R.layout.item_bottom_sheet_room_preview) {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute lateinit var stringProvider: StringProvider
    @EpoxyAttribute lateinit var colorProvider: ColorProvider
    @EpoxyAttribute var izLowPriority: Boolean = false
    @EpoxyAttribute var izFavorite: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var settingsClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var lowPriorityClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var favoriteClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.avatar)
        holder.avatar.onClick(settingsClickListener)
        holder.roomName.setTextOrHide(matrixItem.displayName)
        setLowPriorityState(holder, izLowPriority)
        setFavoriteState(holder, izFavorite)

        holder.roomLowPriority.onClick {
            // Immediate echo
            setLowPriorityState(holder, !izLowPriority)
            if (!izLowPriority) {
                // If we put the room in low priority, it will also remove the favorite tag
                setFavoriteState(holder, false)
            }
            // And do the action
            lowPriorityClickListener?.invoke(it)
        }
        holder.roomFavorite.onClick {
            // Immediate echo
            setFavoriteState(holder, !izFavorite)
            if (!izFavorite) {
                // If we put the room in favorite, it will also remove the low priority tag
                setLowPriorityState(holder, false)
            }
            // And do the action
            favoriteClickListener?.invoke(it)
        }
        holder.roomSettings.apply {
            onClick(settingsClickListener)
            TooltipCompat.setTooltipText(this, stringProvider.getString(CommonStrings.room_list_quick_actions_room_settings))
        }
    }

    private fun setLowPriorityState(holder: Holder, isLowPriority: Boolean) {
        val description: String
        val tintColor: Int
        if (isLowPriority) {
            description = stringProvider.getString(CommonStrings.room_list_quick_actions_low_priority_remove)
            tintColor = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
        } else {
            description = stringProvider.getString(CommonStrings.room_list_quick_actions_low_priority_add)
            tintColor = ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        }
        holder.roomLowPriority.apply {
            contentDescription = description
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            TooltipCompat.setTooltipText(this, description)
        }
    }

    private fun setFavoriteState(holder: Holder, isFavorite: Boolean) {
        val description: String
        val tintColor: Int
        if (isFavorite) {
            description = stringProvider.getString(CommonStrings.room_list_quick_actions_favorite_remove)
            holder.roomFavorite.setImageResource(R.drawable.ic_star_green_24dp)
            tintColor = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
        } else {
            description = stringProvider.getString(CommonStrings.room_list_quick_actions_favorite_add)
            holder.roomFavorite.setImageResource(R.drawable.ic_star_24dp)
            tintColor = ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        }
        holder.roomFavorite.apply {
            contentDescription = description
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            TooltipCompat.setTooltipText(this, description)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val avatar by bind<ImageView>(R.id.bottomSheetRoomPreviewAvatar)
        val roomName by bind<TextView>(R.id.bottomSheetRoomPreviewName)
        val roomLowPriority by bind<ImageView>(R.id.bottomSheetRoomPreviewLowPriority)
        val roomFavorite by bind<ImageView>(R.id.bottomSheetRoomPreviewFavorite)
        val roomSettings by bind<View>(R.id.bottomSheetRoomPreviewSettings)
    }
}
