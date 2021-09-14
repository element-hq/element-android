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

import im.vector.app.core.platform.VectorViewModelAction

sealed class SpaceManageRoomViewAction : VectorViewModelAction {
    data class ToggleSelection(val roomId: String) : SpaceManageRoomViewAction()
    data class UpdateFilter(val filter: String) : SpaceManageRoomViewAction()
    object ClearSelection : SpaceManageRoomViewAction()
    data class MarkAllAsSuggested(val suggested: Boolean) : SpaceManageRoomViewAction()
    object BulkRemove : SpaceManageRoomViewAction()
    object RefreshFromServer : SpaceManageRoomViewAction()
    object LoadAdditionalItemsIfNeeded : SpaceManageRoomViewAction()
}
