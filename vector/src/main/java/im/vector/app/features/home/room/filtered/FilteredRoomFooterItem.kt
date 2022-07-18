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

package im.vector.app.features.home.room.filtered

import android.widget.Button
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass
abstract class FilteredRoomFooterItem : VectorEpoxyModel<FilteredRoomFooterItem.Holder>(R.layout.item_room_filter_footer) {

    @EpoxyAttribute
    var listener: Listener? = null

    @EpoxyAttribute
    var currentFilter: String = ""

    @EpoxyAttribute
    var inSpace: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.createRoomButton.onClick { listener?.createRoom(currentFilter) }
        holder.createDirectChat.onClick { listener?.createDirectChat() }
        holder.openRoomDirectory.onClick { listener?.openRoomDirectory(currentFilter) }

        holder.openRoomDirectory.setText(
                if (inSpace) R.string.space_explore_activity_title else R.string.room_filtering_footer_open_room_directory
        )

        // The explore space screen will have a shortcut to create
        holder.createRoomButton.isVisible = !inSpace
    }

    class Holder : VectorEpoxyHolder() {
        val createRoomButton by bind<Button>(R.id.roomFilterFooterCreateRoom)
        val createDirectChat by bind<Button>(R.id.roomFilterFooterCreateDirect)
        val openRoomDirectory by bind<Button>(R.id.roomFilterFooterOpenRoomDirectory)
    }

    interface Listener {
        fun createRoom(initialName: String)
        fun createDirectChat()
        fun openRoomDirectory(initialFilter: String)
    }
}
