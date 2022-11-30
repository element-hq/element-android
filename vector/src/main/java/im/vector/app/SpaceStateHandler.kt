/*
 * Copyright (c) 2022 New Vector Ltd
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
