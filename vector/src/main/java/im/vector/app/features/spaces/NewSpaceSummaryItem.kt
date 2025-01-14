/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
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
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class NewSpaceSummaryItem : VectorEpoxyModel<NewSpaceSummaryItem.Holder>(R.layout.item_new_space) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var countState: UnreadCounterBadgeView.State = UnreadCounterBadgeView.State.Count(0, false)
    @EpoxyAttribute var expanded: Boolean = false
    @EpoxyAttribute var hasChildren: Boolean = false
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onLongClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onSpaceSelectedListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onToggleExpandListener: ClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        val context = holder.root.context
        holder.root.onClick(onSpaceSelectedListener)
        holder.root.setOnLongClickListener {
            onLongClickListener?.invoke(holder.root)
            true
        }
        holder.name.text = matrixItem.displayName
        holder.root.isChecked = selected

        holder.chevron.setOnClickListener(onToggleExpandListener)
        holder.chevron.isVisible = hasChildren
        holder.chevron.setImageResource(if (expanded) R.drawable.ic_expand_more else R.drawable.ic_arrow_right)
        holder.chevron.contentDescription = context.getString(
                if (expanded) CommonStrings.a11y_collapse_space_children else CommonStrings.a11y_expand_space_children,
                matrixItem.displayName,
        )

        avatarRenderer.render(matrixItem, holder.avatar)
        holder.unreadCounter.render(countState)
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatar)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<CheckableConstraintLayout>(R.id.root)
        val avatar by bind<ImageView>(R.id.avatar)
        val name by bind<TextView>(R.id.name)
        val unreadCounter by bind<UnreadCounterBadgeView>(R.id.unread_counter)
        val chevron by bind<ImageView>(R.id.chevron)
    }
}
