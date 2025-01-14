/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class DaySeparatorItem : VectorEpoxyModel<DaySeparatorItem.Holder>(R.layout.item_timeline_event_day_separator) {

    @EpoxyAttribute lateinit var formattedDay: String

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.dayTextView.text = formattedDay
    }

    class Holder : VectorEpoxyHolder() {
        val dayTextView by bind<TextView>(R.id.itemDayTextView)
    }
}
