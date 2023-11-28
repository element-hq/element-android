/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.provider

/**
 * This interface exists to let the implementation provide localized room display name fallback.
 * The methods can be called when the room has no name, i.e. its `m.room.name` state event does not exist or
 * the name in it is an empty String.
 * It allows the SDK to store the room name fallback into the local storage and so let the client do
 * queries on the room name.
 * *Limitation*: if the locale of the device changes, the methods will not be called again.
 */
interface RoomDisplayNameFallbackProvider {
    /**
     * Return the list of user ids to ignore when computing the room display name.
     */
    fun excludedUserIds(roomId: String): List<String>
    fun getNameForRoomInvite(): String
    fun getNameForEmptyRoom(isDirect: Boolean, leftMemberNames: List<String>): String
    fun getNameFor1member(name: String): String
    fun getNameFor2members(name1: String, name2: String): String
    fun getNameFor3members(name1: String, name2: String, name3: String): String
    fun getNameFor4members(name1: String, name2: String, name3: String, name4: String): String
    fun getNameFor4membersAndMore(name1: String, name2: String, name3: String, remainingCount: Int): String
}
