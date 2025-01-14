/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ui

import im.vector.app.features.home.RoomListDisplayMode

/**
 * This interface is used to persist UI state across application restart.
 */
interface UiStateRepository {

    /**
     * Reset all the saved data.
     */
    fun reset()

    fun getDisplayMode(): RoomListDisplayMode

    fun storeDisplayMode(displayMode: RoomListDisplayMode)

    // TODO Handle SharedPreference per session in a better way, also to cleanup when login out
    fun storeSelectedSpace(spaceId: String?, sessionId: String)
    fun getSelectedSpace(sessionId: String): String?

    fun setCustomRoomDirectoryHomeservers(sessionId: String, servers: Set<String>)
    fun getCustomRoomDirectoryHomeservers(sessionId: String): Set<String>
}
