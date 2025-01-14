/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.explore

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo

sealed class SpaceDirectoryViewAction : VectorViewModelAction {
    data class ExploreSubSpace(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class JoinOrOpen(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class FilterRooms(val query: String?) : SpaceDirectoryViewAction()
    data class ShowDetails(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class NavigateToRoom(val roomId: String) : SpaceDirectoryViewAction()
    object CreateNewRoom : SpaceDirectoryViewAction()
    object HandleBack : SpaceDirectoryViewAction()
    object Retry : SpaceDirectoryViewAction()
    data class RefreshUntilFound(val roomIdToFind: String) : SpaceDirectoryViewAction()
    object LoadAdditionalItemsIfNeeded : SpaceDirectoryViewAction()
}
