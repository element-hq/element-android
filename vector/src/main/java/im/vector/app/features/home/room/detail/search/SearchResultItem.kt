/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.search

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

@EpoxyModelClass(layout = R.layout.item_search_result)
abstract class SearchResultItem : VectorEpoxyModel<SearchResultItem.Holder>() {

    @EpoxyAttribute var avatarRenderer: AvatarRenderer? = null
    @EpoxyAttribute var dateFormatter: VectorDateFormatter? = null
    @EpoxyAttribute var event: Event? = null
    @EpoxyAttribute var sender: User? = null
    @EpoxyAttribute var listener: Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        event ?: return

        holder.view.setOnClickListener { listener?.onItemClicked() }
        sender?.toMatrixItem()?.let { avatarRenderer?.render(it, holder.avatarImageView) }
        holder.memberNameView.text = sender?.displayName
        holder.timeView.text = dateFormatter?.format(event!!.originServerTs, DateFormatKind.MESSAGE_SIMPLE)
        holder.contentView.text = event?.content?.get("body") as? String
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val contentView by bind<TextView>(R.id.messageContentView)
    }

    interface Listener {
        fun onItemClicked()
    }
}
