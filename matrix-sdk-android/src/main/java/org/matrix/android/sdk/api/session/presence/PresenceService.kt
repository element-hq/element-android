/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.matrix.android.sdk.api.session.presence

import org.matrix.android.sdk.api.session.presence.model.PresenceEnum
import org.matrix.android.sdk.api.session.presence.model.UserPresence

/**
 * This interface defines methods for handling user presence information.
 */
interface PresenceService {
    /**
     * Update the presence status for the current user
     * @param presence the new presence state
     * @param statusMsg the status message to attach to this state
     */
    suspend fun setMyPresence(presence: PresenceEnum, statusMsg: String? = null)

    /**
     * Fetch the given user's presence state.
     * @param userId the userId whose presence state to get.
     */
    suspend fun fetchPresence(userId: String): UserPresence

    // TODO Add live data (of Flow) of the presence of a userId
}
