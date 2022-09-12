/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
