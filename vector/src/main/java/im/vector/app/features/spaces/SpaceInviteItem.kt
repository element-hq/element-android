/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import android.widget.ImageView
import android.widget.TextView
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
abstract class SpaceInviteItem : VectorEpoxyModel<SpaceInviteItem.Holder>(R.layout.item_space_invite) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var inviter: String = ""
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onLongClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onInviteSelectedListener: ClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        val context = holder.root.context
        holder.root.isChecked = selected
        holder.root.onClick(onInviteSelectedListener)
        holder.root.setOnLongClickListener { onLongClickListener?.invoke(holder.root).let { true } }
        holder.name.text = matrixItem.displayName
        holder.invitedBy.text = context.getString(CommonStrings.invited_by, inviter)

        avatarRenderer.render(matrixItem, holder.avatar)
        holder.notificationBadge.render(UnreadCounterBadgeView.State.Text("!", true))
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatar)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<CheckableConstraintLayout>(R.id.root)
        val avatar by bind<ImageView>(R.id.avatar)
        val name by bind<TextView>(R.id.name)
        val invitedBy by bind<TextView>(R.id.invited_by)
        val notificationBadge by bind<UnreadCounterBadgeView>(R.id.notification_badge)
    }
}
