/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class RoomSummaryPlaceHolderItem : VectorEpoxyModel<RoomSummaryPlaceHolderItem.Holder>(R.layout.item_room_placeholder) {

    @EpoxyAttribute
    var useSingleLineForLastEvent: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        if (useSingleLineForLastEvent) {
            holder.subtitleView.setLines(1)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val subtitleView by bind<TextView>(R.id.subtitleView)
    }
}
