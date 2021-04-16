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

package im.vector.app.features.spaces.manage

import androidx.recyclerview.widget.DiffUtil
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class AddRoomListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler(),

        itemDiffCallback = object : DiffUtil.ItemCallback<RoomSummary>() {

            override fun areItemsTheSame(oldItem: RoomSummary, newItem: RoomSummary): Boolean {
                return oldItem.roomId == newItem.roomId
            }

            override fun areContentsTheSame(oldItem: RoomSummary, newItem: RoomSummary): Boolean {
                // for this use case we can test less things
                return oldItem.displayName == newItem.displayName
                        && oldItem.avatarUrl == newItem.avatarUrl
            }
        }
) {

    interface Listener {
        fun onItemSelected(roomSummary: RoomSummary)
    }

    var listener: Listener? = null
    var ignoreRooms: List<String>? = null

    var selectedItems: Map<String, Boolean> = emptyMap()
        set(value) {
            field = value
            // mmm annoying but can't just force for a given model
            requestForcedModelBuild()
        }

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (ignoreRooms == null) {
            super.addModels(models)
        } else {
            super.addModels(
                    models.filter {
                        it !is RoomSelectionItem ||  !ignoreRooms!!.contains(it.matrixItem.id)
                    }
            )
        }
    }

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        if (item == null) return RoomSelectionPlaceHolderItem_().apply { id(currentPosition) }
        return RoomSelectionItem_().apply {
            id(item.roomId)
            matrixItem(item.toMatrixItem())
            avatarRenderer(this@AddRoomListController.avatarRenderer)
            space(item.roomType == RoomType.SPACE)
            selected(selectedItems[item.roomId] ?: false)
            itemClickListener(DebouncedClickListener({
                listener?.onItemSelected(item)
            }))
        }
    }
}
