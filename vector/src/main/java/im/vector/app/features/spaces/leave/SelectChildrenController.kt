/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.leave

import androidx.core.util.Predicate
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.spaces.manage.roomSelectionItem
import im.vector.lib.strings.CommonStrings
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
            is Loading -> {
                loadingItem {
                    id("loading")
                }
            }
            is Success -> {
                matchFilter.filter = data.currentFilter
                val roomList = children.invoke().filter { matchFilter.test(it) }

                if (roomList.isEmpty()) {
                    noResultItem {
                        id("empty")
                        text(host.stringProvider.getString(CommonStrings.no_result_placeholder))
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
            is Fail -> {
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
