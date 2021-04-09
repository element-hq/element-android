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

package org.matrix.android.sdk.common

import org.matrix.android.sdk.api.RoomDisplayNameFallbackProvider

class TestRoomDisplayNameFallbackProvider : RoomDisplayNameFallbackProvider {

    override fun getNameForRoomInvite() =
            "Room invite"

    override fun getNameForEmptyRoom(isDirect: Boolean, leftMemberNames: List<String>) =
            "Empty room"

    override fun getNameFor1member(name: String) =
            name

    override fun getNameFor2members(name1: String, name2: String) =
            "$name1 and $name2"

    override fun getNameFor3members(name1: String, name2: String, name3: String) =
            "$name1, $name2 and $name3"

    override fun getNameFor4members(name1: String, name2: String, name3: String, name4: String) =
            "$name1, $name2, $name3 and $name4"

    override fun getNameFor4membersAndMore(name1: String, name2: String, name3: String, remainingCount: Int) =
            "$name1, $name2, $name3 and $remainingCount others"
}
