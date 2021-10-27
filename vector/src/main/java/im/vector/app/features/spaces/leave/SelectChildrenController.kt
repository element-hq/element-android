/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.leave

import androidx.core.util.Predicate
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.spaces.manage.roomSelectionItem
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SelectChildrenController @Inject constructor(
        val avatarRenderer: AvatarRenderer,
        val stringProvider: StringProvider
) : TypedEpoxyController<SpaceLeaveAdvanceViewState>() {

    interface Listener {
        fun onItemSelected(roomSummary: RoomSummary)
    }

    var listener: Listener? = null
    private val matchFilter = RoomSearchMatchFilter()
    override fun buildModels(data: SpaceLeaveAdvanceViewState?) {
        val children = data?.allChildren ?: return
        val host = this
        when (children) {
            Uninitialized -> return
            is Loading    -> {
                loadingItem {
                    id("loading")
                }
            }
            is Success    -> {
                matchFilter.filter = data.currentFilter
                val roomList = children.invoke().filter { matchFilter.test(it) }

                if (roomList.isEmpty()) {
                    noResultItem {
                        id("empty")
                        text(host.stringProvider.getString(R.string.no_result_placeholder))
                    }
                } else {
                    roomList.forEach { item ->
                        roomSelectionItem {
                            id(item.roomId)
                            matrixItem(item.toMatrixItem())
                            avatarRenderer(host.avatarRenderer)
                            selected(data.selectedRooms.contains(item.roomId))
                            itemClickListener {
                                host.listener?.onItemSelected(item)
                            }
                        }
                    }
                }
            }
            is Fail       -> {
//                errorWithRetryItem {
//                    id("failed_to_load")
//                }
            }
        }
    }

    class RoomSearchMatchFilter : Predicate<RoomSummary> {
        var filter: String = ""

        override fun test(roomSummary: RoomSummary): Boolean {
            if (filter.isEmpty()) {
                // No filter
                return true
            }
            // if filter is "Jo Do", it should match "John Doe"
            return filter.split(" ").all {
                roomSummary.name.contains(it, ignoreCase = true).orFalse() ||
                        roomSummary.topic.contains(it, ignoreCase = true).orFalse()
            }
        }
    }
}
