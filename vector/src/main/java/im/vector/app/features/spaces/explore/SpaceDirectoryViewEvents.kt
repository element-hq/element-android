/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.explore

import im.vector.app.core.platform.VectorViewEvents

sealed class SpaceDirectoryViewEvents : VectorViewEvents {
    object Dismiss : SpaceDirectoryViewEvents()
    data class NavigateToRoom(val roomId: String) : SpaceDirectoryViewEvents()
    data class NavigateToMxToBottomSheet(val link: String) : SpaceDirectoryViewEvents()
    data class NavigateToCreateNewRoom(val currentSpaceId: String) : SpaceDirectoryViewEvents()
}
