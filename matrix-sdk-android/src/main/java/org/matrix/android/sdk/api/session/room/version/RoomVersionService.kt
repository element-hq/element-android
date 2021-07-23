/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.version

interface RoomVersionService {
    /**
     * Return the room version of this room
     */
    fun getRoomVersion(): String

    /**
     * Upgrade to the given room version
     * @return the replacement room id
     */
    suspend fun upgradeToVersion(version: String): String

    /**
     * Get the recommended room version for the current homeserver
     */
    fun getRecommendedVersion() : String

    /**
     * Ask if the user has enough power level to upgrade the room
     */
    fun userMayUpgradeRoom(userId: String): Boolean

    /**
     * Return true if the current room version is declared unstable by the homeserver
     */
    fun isUsingUnstableRoomVersion(): Boolean
}
