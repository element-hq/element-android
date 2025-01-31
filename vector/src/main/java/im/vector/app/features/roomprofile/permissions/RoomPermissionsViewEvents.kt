/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for room settings screen.
 */
sealed class RoomPermissionsViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : RoomPermissionsViewEvents()
    object Success : RoomPermissionsViewEvents()
}
