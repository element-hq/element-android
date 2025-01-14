/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for room directory screen.
 */
sealed class RoomDirectoryViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : RoomDirectoryViewEvents()
}
