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

package im.vector.app.features.spaces.create

import im.vector.app.core.platform.VectorViewEvents

sealed class CreateSpaceEvents : VectorViewEvents {
    object NavigateToDetails : CreateSpaceEvents()
    object NavigateToChooseType : CreateSpaceEvents()
    object NavigateToAddRooms : CreateSpaceEvents()
    object NavigateToAdd3Pid : CreateSpaceEvents()
    object NavigateToChoosePrivateType : CreateSpaceEvents()
    object Dismiss : CreateSpaceEvents()
    data class FinishSuccess(val spaceId: String, val defaultRoomId: String?, val topology: SpaceTopology?) : CreateSpaceEvents()
    data class ShowModalError(val errorMessage: String) : CreateSpaceEvents()
    object HideModalLoading : CreateSpaceEvents()
    data class ShowModalLoading(val message: String?) : CreateSpaceEvents()
}
