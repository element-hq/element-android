/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.home.room.breadcrumbs

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_breadcrumbs)
abstract class BreadcrumbsItem : VectorEpoxyModel<BreadcrumbsItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var roomId: String
    @EpoxyAttribute lateinit var roomName: CharSequence
    @EpoxyAttribute var avatarUrl: String? = null
    // TODO @EpoxyAttribute var unreadNotificationCount: Int = 0
    // TODO @EpoxyAttribute var hasUnreadMessage: Boolean = false
    // TODO @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var itemClickListener: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener(itemClickListener)
        avatarRenderer.render(avatarUrl, roomId, roomName.toString(), holder.avatarImageView)
    }

    class Holder : VectorEpoxyHolder() {
        // TODO val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView)
        // TODO val unreadIndentIndicator by bind<View>(R.id.roomUnreadIndicator)
        val avatarImageView by bind<ImageView>(R.id.breadcrumbsImageView)
        val rootView by bind<ViewGroup>(R.id.breadcrumbsRoot)
    }
}
