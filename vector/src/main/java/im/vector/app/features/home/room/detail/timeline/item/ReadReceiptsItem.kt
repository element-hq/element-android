/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.ReadReceiptsView
import im.vector.app.features.home.AvatarRenderer

@EpoxyModelClass
abstract class ReadReceiptsItem : VectorEpoxyModel<ReadReceiptsItem.Holder>(R.layout.item_timeline_event_read_receipts), ItemWithEvents {

    @EpoxyAttribute lateinit var eventId: String
    @EpoxyAttribute lateinit var readReceipts: List<ReadReceiptData>
    @EpoxyAttribute var shouldHideReadReceipts: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var clickListener: ClickListener

    override fun canAppendReadMarker(): Boolean = false

    override fun getEventIds(): List<String> = listOf(eventId)

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.readReceiptsView.onClick(clickListener)
        holder.readReceiptsView.render(readReceipts, avatarRenderer)
        holder.readReceiptsView.isVisible = !shouldHideReadReceipts
    }

    override fun unbind(holder: Holder) {
        holder.readReceiptsView.unbind(avatarRenderer)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val readReceiptsView by bind<ReadReceiptsView>(R.id.readReceiptsView)
    }
}
