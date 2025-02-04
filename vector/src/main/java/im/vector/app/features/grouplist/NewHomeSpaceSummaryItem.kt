/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.grouplist

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.CheckableConstraintLayout
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings

@EpoxyModelClass
abstract class NewHomeSpaceSummaryItem : VectorEpoxyModel<NewHomeSpaceSummaryItem.Holder>(R.layout.item_new_space) {

    @EpoxyAttribute var text: String = ""
    @EpoxyAttribute var selected: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null
    @EpoxyAttribute var countState: UnreadCounterBadgeView.State = UnreadCounterBadgeView.State.Count(0, false)
    @EpoxyAttribute var showSeparator: Boolean = false

    override fun getViewType() = R.id.space_item_home

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.root.onClick(listener)
        holder.name.text = holder.view.context.getString(CommonStrings.all_chats)
        holder.root.isChecked = selected
        holder.root.context.resources
        holder.avatar.background = ContextCompat.getDrawable(holder.view.context, R.drawable.new_space_home_background)
        holder.avatar.backgroundTintList = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                        ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_tertiary), (255 * 0.3).toInt()
                )
        )
        holder.avatar.setImageResource(R.drawable.ic_space_home)
        holder.avatar.imageTintList = ColorStateList.valueOf(ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        holder.avatar.scaleType = ImageView.ScaleType.CENTER_INSIDE

        holder.unreadCounter.render(countState)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<CheckableConstraintLayout>(R.id.root)
        val avatar by bind<ImageView>(R.id.avatar)
        val name by bind<TextView>(R.id.name)
        val unreadCounter by bind<UnreadCounterBadgeView>(R.id.unread_counter)
    }
}
