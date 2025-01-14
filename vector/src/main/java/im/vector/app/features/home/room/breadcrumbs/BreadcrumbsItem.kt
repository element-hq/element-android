/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.breadcrumbs

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class BreadcrumbsItem : VectorEpoxyModel<BreadcrumbsItem.Holder>(R.layout.item_breadcrumbs) {

    @EpoxyAttribute var hasTypingUsers: Boolean = false
    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var unreadNotificationCount: Int = 0
    @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var hasUnreadMessage: Boolean = false
    @EpoxyAttribute var hasDraft: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.onClick(itemClickListener)
        holder.unreadIndentIndicator.isVisible = hasUnreadMessage
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.avatarImageView.contentDescription = matrixItem.getBestName()
        holder.unreadCounterBadgeView.render(UnreadCounterBadgeView.State.Count(unreadNotificationCount, showHighlighted))
        holder.draftIndentIndicator.isVisible = hasDraft
        holder.typingIndicator.isVisible = hasTypingUsers
    }

    class Holder : VectorEpoxyHolder() {
        val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.breadcrumbsUnreadCounterBadgeView)
        val unreadIndentIndicator by bind<View>(R.id.breadcrumbsUnreadIndicator)
        val draftIndentIndicator by bind<View>(R.id.breadcrumbsDraftBadge)
        val typingIndicator by bind<View>(R.id.breadcrumbsTypingView)
        val avatarImageView by bind<ImageView>(R.id.breadcrumbsImageView)
        val rootView by bind<ViewGroup>(R.id.breadcrumbsRoot)
    }
}
