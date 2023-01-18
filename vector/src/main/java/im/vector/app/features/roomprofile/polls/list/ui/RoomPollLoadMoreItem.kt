/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.list.ui

import android.widget.Button
import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass
abstract class RoomPollLoadMoreItem : VectorEpoxyModel<RoomPollLoadMoreItem.Holder>(R.layout.item_poll_load_more) {

    @EpoxyAttribute
    var loadingMore: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.loadMoreButton.isEnabled = loadingMore.not()
        holder.loadMoreButton.onClick(clickListener)
        holder.loadMoreProgressBar.isVisible = loadingMore
    }

    class Holder : VectorEpoxyHolder() {
        val loadMoreButton by bind<Button>(R.id.roomPollsLoadMore)
        val loadMoreProgressBar by bind<ProgressBar>(R.id.roomPollsLoadMoreProgress)
    }
}
