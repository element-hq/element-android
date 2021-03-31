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

package im.vector.app.features.home.room.list

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.core.utils.createUIHandler
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class RoomSummaryPagedControllerFactory @Inject constructor(private val stringProvider: StringProvider,
                                                            private val userPreferencesProvider: UserPreferencesProvider,
                                                            private val roomSummaryItemFactory: RoomSummaryItemFactory) {

    fun createRoomSummaryPagedController(): RoomSummaryPagedController {
        return RoomSummaryPagedController(stringProvider, userPreferencesProvider, roomSummaryItemFactory)
    }
}

class RoomSummaryPagedController constructor(private val stringProvider: StringProvider,
                                             private val userPreferencesProvider: UserPreferencesProvider,
                                             private val roomSummaryItemFactory: RoomSummaryItemFactory)
    : PagedListEpoxyController<RoomSummary>(
// Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    var listener: RoomListListener? = null

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        val unwrappedItem = item
                // for place holder if enabled
                ?: return roomSummaryItemFactory.createRoomItem(
                        RoomSummary(
                                roomId = "null_item_pos_$currentPosition",
                                name = "",
                                encryptionEventTs = null,
                                isEncrypted = false,
                                typingUsers = emptyList()
                        ), emptySet(), null, null)

//        GenericItem_().apply { id("null_item_pos_$currentPosition") }

        return roomSummaryItemFactory.create(unwrappedItem, emptyMap(), emptySet(), listener)
    }

//    override fun onModelBound(holder: EpoxyViewHolder, boundModel: EpoxyModel<*>, position: Int, previouslyBoundModel: EpoxyModel<*>?) {
//        Timber.w("VAL: Will load around $position")
//        super.onModelBound(holder, boundModel, position, previouslyBoundModel)
//    }
//    fun onRoomLongClicked() {
//        userPreferencesProvider.neverShowLongClickOnRoomHelpAgain()
//        requestModelBuild()
//    }
}
