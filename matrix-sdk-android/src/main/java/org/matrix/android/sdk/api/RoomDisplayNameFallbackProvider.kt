/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api

/**
 * This interface exists to let the implementation provide localized room display name fallback.
 * The methods can be called when the room has no name, i.e. its `m.room.name` state event does not exist or
 * the name in it is an empty String.
 * It allows the SDK to store the room name fallback into the local storage and so let the client do
 * queries on the room name.
 * *Limitation*: if the locale of the device changes, the methods will not be called again.
 */
interface RoomDisplayNameFallbackProvider {
    fun getNameForRoomInvite(): String
    fun getNameForEmptyRoom(isDirect: Boolean, leftMemberNames: List<String>): String
    fun getNameFor1member(name: String): String
    fun getNameFor2members(name1: String, name2: String): String
    fun getNameFor3members(name1: String, name2: String, name3: String): String
    fun getNameFor4members(name1: String, name2: String, name3: String, name4: String): String
    fun getNameFor4membersAndMore(name1: String, name2: String, name3: String, remainingCount: Int): String
}
