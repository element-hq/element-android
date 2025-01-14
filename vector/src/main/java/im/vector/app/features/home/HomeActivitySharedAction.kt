/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.core.platform.VectorSharedAction

/**
 * Supported navigation actions for [HomeActivity].
 */
sealed class HomeActivitySharedAction : VectorSharedAction {
    object OpenDrawer : HomeActivitySharedAction()
    object CloseDrawer : HomeActivitySharedAction()
    object OnCloseSpace : HomeActivitySharedAction()
    object AddSpace : HomeActivitySharedAction()
    data class OpenSpacePreview(val spaceId: String) : HomeActivitySharedAction()
    data class OpenSpaceInvite(val spaceId: String) : HomeActivitySharedAction()
    data class ShowSpaceSettings(val spaceId: String) : HomeActivitySharedAction()
    object SendSpaceFeedBack : HomeActivitySharedAction()
}
