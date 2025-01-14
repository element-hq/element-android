/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.databinding.ItemRoomCategoryBinding
import im.vector.app.features.themes.ThemeUtils

class SectionHeaderAdapter constructor(
        roomsSectionData: RoomsSectionData,
        private val onClickAction: ClickListener
) : RecyclerView.Adapter<SectionHeaderAdapter.VH>() {

    data class RoomsSectionData(
            val name: String,
            val itemCount: Int = 0,
            val isExpanded: Boolean = true,
            val notificationCount: Int = 0,
            val isHighlighted: Boolean = false,
            val isHidden: Boolean = true,
            // This will be false until real data has been submitted once
            val isLoading: Boolean = true,
            val isCollapsable: Boolean = false
    )

    var roomsSectionData: RoomsSectionData = roomsSectionData
        private set

    fun updateSection(block: (RoomsSectionData) -> RoomsSectionData) {
        val newRoomsSectionData = block(roomsSectionData)
        if (roomsSectionData != newRoomsSectionData) {
            roomsSectionData = newRoomsSectionData
            notifyDataSetChanged()
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = roomsSectionData.hashCode().toLong()

    override fun getItemViewType(position: Int) = R.layout.item_room_category

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH.create(parent, onClickAction)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(roomsSectionData)
    }

    override fun getItemCount(): Int = if (roomsSectionData.isHidden) 0 else 1

    class VH constructor(
            private val binding: ItemRoomCategoryBinding,
            onClickAction: ClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.onClick(onClickAction)
        }

        fun bind(roomsSectionData: RoomsSectionData) {
            binding.roomCategoryTitleView.text = roomsSectionData.name
            val tintColor = ThemeUtils.getColor(binding.root.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
            val collapsableArrowDrawable: Drawable? = if (roomsSectionData.isCollapsable) {
                val expandedArrowDrawableRes = if (roomsSectionData.isExpanded) R.drawable.ic_expand_more else R.drawable.ic_expand_less
                ContextCompat.getDrawable(binding.root.context, expandedArrowDrawableRes)?.also {
                    DrawableCompat.setTint(it, tintColor)
                }
            } else {
                null
            }
            binding.root.isClickable = roomsSectionData.isCollapsable
            binding.roomCategoryCounterView.setCompoundDrawablesWithIntrinsicBounds(null, null, collapsableArrowDrawable, null)
            binding.roomCategoryCounterView.text = if (roomsSectionData.itemCount > 0) "${roomsSectionData.itemCount}" else null
            binding.roomCategoryUnreadCounterBadgeView.render(
                    UnreadCounterBadgeView.State.Count(roomsSectionData.notificationCount, roomsSectionData.isHighlighted)
            )
        }

        companion object {
            fun create(parent: ViewGroup, onClickAction: ClickListener): VH {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_room_category, parent, false)
                val binding = ItemRoomCategoryBinding.bind(view)
                return VH(binding, onClickAction)
            }
        }
    }
}
