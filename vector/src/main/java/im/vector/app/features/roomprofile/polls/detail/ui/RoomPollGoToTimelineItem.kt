/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import android.widget.Button
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass
abstract class RoomPollGoToTimelineItem : VectorEpoxyModel<RoomPollGoToTimelineItem.Holder>(R.layout.item_poll_go_to_timeline) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.goToTimelineButton.onClick(clickListener)
    }

    class Holder : VectorEpoxyHolder() {
        val goToTimelineButton by bind<Button>(R.id.roomPollGoToTimeline)
    }
}
