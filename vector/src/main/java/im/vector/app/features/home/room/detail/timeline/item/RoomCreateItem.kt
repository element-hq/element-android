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
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import me.saket.bettermovementmethod.BetterLinkMovementMethod

@EpoxyModelClass
abstract class RoomCreateItem : VectorEpoxyModel<RoomCreateItem.Holder>(R.layout.item_timeline_event_create) {

    @EpoxyAttribute lateinit var text: EpoxyCharSequence

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.description.movementMethod = BetterLinkMovementMethod.getInstance()
        holder.description.text = text.charSequence
    }

    class Holder : VectorEpoxyHolder() {
        val description by bind<TextView>(R.id.roomCreateItemDescription)
    }
}
