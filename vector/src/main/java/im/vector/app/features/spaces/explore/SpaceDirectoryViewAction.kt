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

package im.vector.app.features.spaces.explore

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo

sealed class SpaceDirectoryViewAction : VectorViewModelAction {
    data class ExploreSubSpace(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class JoinOrOpen(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class ShowDetails(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class NavigateToRoom(val roomId: String) : SpaceDirectoryViewAction()
    object CreateNewRoom : SpaceDirectoryViewAction()
    object HandleBack : SpaceDirectoryViewAction()
    object Retry : SpaceDirectoryViewAction()
    data class RefreshUntilFound(val roomIdToFind: String) : SpaceDirectoryViewAction()
    object LoadAdditionalItemsIfNeeded : SpaceDirectoryViewAction()
}
