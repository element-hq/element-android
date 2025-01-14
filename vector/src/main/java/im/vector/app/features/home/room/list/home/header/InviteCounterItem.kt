/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.header

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.home.room.list.UnreadCounterBadgeView

@EpoxyModelClass
abstract class InviteCounterItem : VectorEpoxyModel<InviteCounterItem.Holder>(R.layout.item_invites_count) {

    @EpoxyAttribute var invitesCount: Int = 0
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.setOnClickListener(listener)
        holder.unreadCounterBadgeView.render(UnreadCounterBadgeView.State.Count(invitesCount, true))
    }

    class Holder : VectorEpoxyHolder() {
        val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.invites_count_badge)
    }
}
