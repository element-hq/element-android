/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.sqldelight.session

import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test

class RoomSummaryTests : SessionTests {

    @Test
    fun test_all_operations() {
        val database = sessionDatabase()
        database.roomSummaryQueries.getAllWithMemberships(listOf(Memberships.JOIN)).executeAsList() shouldHaveSize 0
        val roomSummaryEntity = createRoomSummaryEntity("room1")
        database.roomSummaryQueries.insertOrUpdate(roomSummaryEntity)
        database.roomSummaryQueries.getAllWithMemberships(listOf(Memberships.JOIN)).executeAsList() shouldHaveSize 1
        database.roomSummaryQueries.insertOrUpdate(roomSummaryEntity)
        database.roomSummaryQueries.getAllWithMemberships(listOf(Memberships.JOIN)).executeAsList() shouldHaveSize 1
        database.roomSummaryQueries.getAll().executeAsList() shouldHaveSize 1
        database.roomSummaryQueries.get("room1").executeAsOneOrNull()?.shouldNotBeNull()
    }

    private fun createRoomSummaryEntity(roomId: String): RoomSummaryEntity {
        return RoomSummaryEntity.Impl(
                room_id = roomId,
                membership = Memberships.JOIN,
                avatar_url = "",
                display_name = "",
                invited_members_count = 0,
                topic = "",
                joined_members_count = 0,
                latest_previewable_event = null,
                is_direct = false,
                notification_count = 0,
                highlight_count = 0,
                canonical_alias = null,
                is_encrypted = false,
                has_unread = false,
                direct_user_id = null
        )
    }


}
