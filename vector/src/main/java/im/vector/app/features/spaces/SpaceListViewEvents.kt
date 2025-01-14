/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for group list screen.
 */
sealed class SpaceListViewEvents : VectorViewEvents {
    data class OpenSpaceSummary(val id: String) : SpaceListViewEvents()
    data class OpenSpaceInvite(val id: String) : SpaceListViewEvents()
    object AddSpace : SpaceListViewEvents()
    object CloseDrawer : SpaceListViewEvents()
}
