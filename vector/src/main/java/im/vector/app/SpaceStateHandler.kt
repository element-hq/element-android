/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional

/**
 * Gets info about the current space the user has navigated to, any space backstack they may have
 * and handles switching to different spaces.
 */
interface SpaceStateHandler : DefaultLifecycleObserver {

    /**
     * Gets the current space the current user has navigated to.
     *
     * @return null if the user is not in
     */
    fun getCurrentSpace(): RoomSummary?

    /**
     * Sets the new space the current user is navigating to.
     *
     * @param spaceId the id of the space being navigated to
     * @param session the current active session
     * @param persistNow if true, the current space will immediately be persisted in shared prefs
     * @param isForwardNavigation whether this navigation is a forward action to properly handle backstack
     */
    fun setCurrentSpace(
            spaceId: String?,
            session: Session? = null,
            persistNow: Boolean = false,
            isForwardNavigation: Boolean = true,
    )

    /**
     * Gets the Space ID of the space on top of the backstack.
     *
     * May return null to indicate the All Chats space.
     */
    fun popSpaceBackstack(): String?

    fun getSpaceBackstack(): List<String?>

    /**
     * Gets a flow of the selected space for clients to react immediately to space changes.
     */
    fun getSelectedSpaceFlow(): Flow<Optional<RoomSummary>>

    /**
     * Gets the id of the active space, or null if there is none.
     */
    fun getSafeActiveSpaceId(): String?
}
