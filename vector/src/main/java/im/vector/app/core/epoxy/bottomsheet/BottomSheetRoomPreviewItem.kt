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
 *
 */
package im.vector.app.core.epoxy.bottomsheet

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.util.MatrixItem

/**
 * A room preview for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_bottom_sheet_room_preview)
abstract class BottomSheetRoomPreviewItem : VectorEpoxyModel<BottomSheetRoomPreviewItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute lateinit var stringProvider: StringProvider
    @EpoxyAttribute var izLowPriority: Boolean = false
    @EpoxyAttribute var izFavorite: Boolean = false
    @EpoxyAttribute var settingsClickListener: ClickListener? = null
    @EpoxyAttribute var lowPriorityClickListener: ClickListener? = null
    @EpoxyAttribute var favoriteClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.avatar)
        holder.avatar.onClick(settingsClickListener)
        holder.roomName.setTextOrHide(matrixItem.displayName)
        setLowPriorityState(holder, izLowPriority)
        setFavoriteState(holder, izFavorite)

        holder.roomLowPriority.setOnClickListener {
            // Immediate echo
            setLowPriorityState(holder, !izLowPriority)
            if (!izLowPriority) {
                // If we put the room in low priority, it will also remove the favorite tag
                setFavoriteState(holder, false)
            }
            // And do the action
            lowPriorityClickListener?.invoke()
        }
        holder.roomFavorite.setOnClickListener {
            // Immediate echo
            setFavoriteState(holder, !izFavorite)
            if (!izFavorite) {
                // If we put the room in favorite, it will also remove the low priority tag
                setLowPriorityState(holder, false)
            }
            // And do the action
            favoriteClickListener?.invoke()
        }
        holder.roomSettings.onClick(settingsClickListener)
    }

    private fun setLowPriorityState(holder: Holder, isLowPriority: Boolean) {
        val tintColor: Int
        if (isLowPriority) {
            holder.roomLowPriority.contentDescription = stringProvider.getString(R.string.room_list_quick_actions_low_priority_remove)
            tintColor = ContextCompat.getColor(holder.view.context, R.color.riotx_accent)
        } else {
            holder.roomLowPriority.contentDescription = stringProvider.getString(R.string.room_list_quick_actions_low_priority_add)
            tintColor = ThemeUtils.getColor(holder.view.context, R.attr.riotx_text_secondary)
        }
        ImageViewCompat.setImageTintList(holder.roomLowPriority, ColorStateList.valueOf(tintColor))
    }

    private fun setFavoriteState(holder: Holder, isFavorite: Boolean) {
        val tintColor: Int
        if (isFavorite) {
            holder.roomFavorite.contentDescription = stringProvider.getString(R.string.room_list_quick_actions_favorite_remove)
            holder.roomFavorite.setImageResource(R.drawable.ic_star_green_24dp)
            tintColor = ContextCompat.getColor(holder.view.context, R.color.riotx_accent)
        } else {
            holder.roomFavorite.contentDescription = stringProvider.getString(R.string.room_list_quick_actions_favorite_add)
            holder.roomFavorite.setImageResource(R.drawable.ic_star_24dp)
            tintColor = ThemeUtils.getColor(holder.view.context, R.attr.riotx_text_secondary)
        }
        ImageViewCompat.setImageTintList(holder.roomFavorite, ColorStateList.valueOf(tintColor))
    }

    class Holder : VectorEpoxyHolder() {
        val avatar by bind<ImageView>(R.id.bottomSheetRoomPreviewAvatar)
        val roomName by bind<TextView>(R.id.bottomSheetRoomPreviewName)
        val roomLowPriority by bind<ImageView>(R.id.bottomSheetRoomPreviewLowPriority)
        val roomFavorite by bind<ImageView>(R.id.bottomSheetRoomPreviewFavorite)
        val roomSettings by bind<View>(R.id.bottomSheetRoomPreviewSettings)
    }
}
