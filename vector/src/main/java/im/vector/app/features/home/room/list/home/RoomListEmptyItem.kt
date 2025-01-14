/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.platform.StateView

@EpoxyModelClass
abstract class RoomListEmptyItem : VectorEpoxyModel<RoomListEmptyItem.Holder>(R.layout.item_state_view) {

    @EpoxyAttribute
    lateinit var emptyData: StateView.State.Empty

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.stateView.state = emptyData
    }

    class Holder : VectorEpoxyHolder() {
        val stateView by bind<StateView>(R.id.stateView)
    }
}
