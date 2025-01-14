/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.CheckableConstraintLayout
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class SubSpaceSummaryItem : VectorEpoxyModel<SubSpaceSummaryItem.Holder>(R.layout.item_sub_space) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var selected: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onMore: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var toggleExpand: ClickListener? = null
    @EpoxyAttribute var expanded: Boolean = false
    @EpoxyAttribute var hasChildren: Boolean = false
    @EpoxyAttribute var indent: Int = 0
    @EpoxyAttribute var countState: UnreadCounterBadgeView.State = UnreadCounterBadgeView.State.Count(0, false)

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.onClick(listener)
        holder.groupNameView.text = matrixItem.displayName
        holder.rootView.isChecked = selected
        holder.moreView.isVisible = onMore != null
        holder.moreView.onClick(onMore)

        holder.collapseIndicator.setImageDrawable(
                ContextCompat.getDrawable(
                        holder.view.context,
                        if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
        )
        holder.collapseIndicator.onClick(toggleExpand)
        holder.collapseIndicator.isVisible = hasChildren

        holder.indentSpace.isVisible = indent > 0
        holder.indentSpace.updateLayoutParams {
            width = indent * 30
        }

        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.counterBadgeView.render(countState)
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.groupAvatarImageView)
        val groupNameView by bind<TextView>(R.id.groupNameView)
        val rootView by bind<CheckableConstraintLayout>(R.id.itemGroupLayout)
        val moreView by bind<ImageView>(R.id.groupTmpLeave)
        val collapseIndicator by bind<ImageView>(R.id.groupChildrenCollapse)
        val indentSpace by bind<Space>(R.id.indent)
        val counterBadgeView by bind<UnreadCounterBadgeView>(R.id.groupCounterBadge)
    }
}
