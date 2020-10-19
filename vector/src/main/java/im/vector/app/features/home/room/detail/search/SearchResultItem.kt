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
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_search_result)
abstract class SearchResultItem : VectorEpoxyModel<SearchResultItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var dateFormatter: VectorDateFormatter? = null
    @EpoxyAttribute lateinit var event: Event
    @EpoxyAttribute var sender: MatrixItem? = null
    @EpoxyAttribute var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.view.onClick(listener)
        sender?.let { avatarRenderer.render(it, holder.avatarImageView) }
        holder.memberNameView.setTextOrHide(sender?.getBestName())
        holder.timeView.text = dateFormatter?.format(event.originServerTs, DateFormatKind.MESSAGE_SIMPLE)
        // TODO Improve that (use formattedBody, etc.)
        holder.contentView.text = event.content?.get("body") as? String
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val contentView by bind<TextView>(R.id.messageContentView)
    }
}
