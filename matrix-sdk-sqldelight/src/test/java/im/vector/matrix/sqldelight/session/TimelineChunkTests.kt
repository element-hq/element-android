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

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBe
import org.junit.Test

class TimelineChunkTests : SessionTests {

    private val roomId = "roomId"

    @Test
    fun test_all_operations() {
        val database = sessionDatabase()
        val chunkQueries = database.chunkQueries
        val timelineEventQueries = database.timelineEventQueries

        chunkQueries.getLastLive(roomId).executeAsOneOrNull() shouldBe null
        chunkQueries.insert(roomId, null, prev_token = "prev", is_last_forward = true, is_last_backard = false)
        val liveChunk = chunkQueries.getLastLive(roomId).executeAsOne()
        chunkQueries.lastInsertId().executeAsOne() `should be equal to` liveChunk.chunk_id
        timelineEventQueries.getAllFromRoom(roomId = roomId).executeAsList() shouldHaveSize 0

        val insertCount = 100
        database.transaction {
            (0 until insertCount).forEach {
                val event = createEvent(it)
                database.eventQueries.insert(event)
                createTimelineEvent(database, event.event_id, liveChunk.chunk_id)
            }
            timelineEventQueries.getAllFromRoom(roomId = roomId).executeAsList() shouldHaveSize insertCount
        }
        timelineEventQueries.getAllFromChunk(chunk_id = liveChunk.chunk_id).executeAsList() shouldHaveSize insertCount
        timelineEventQueries.getPagedFromLiveChunk(roomId, 100, 0).executeAsList() shouldHaveSize insertCount
    }

    @Test
    fun insert_event_with_existing_id_should_ignore() {
        val database = sessionDatabase()
        database.transaction {
            database.eventQueries.insert(createEvent(0))
            database.eventQueries.exist(generateEventId(0), roomId).executeAsOneOrNull() shouldNotBe null
            database.eventQueries.insert(createEvent(0))
            database.eventQueries.exist(generateEventId(0), roomId).executeAsList() shouldHaveSize 1
        }
    }


    private fun generateEventId(id: Int) = "eventId_$id"

    private fun createTimelineEvent(database: SessionDatabase, eventId: String, chunkId: Long) {
        database.timelineEventQueries.insert(
                room_id = roomId,
                event_id = eventId,
                chunk_id = chunkId,
                display_index = 0,
                is_unique_display_name = false,
                sender_avatar = null,
                sender_name = "Tester"
        )
    }

    private fun createEvent(eventId: Int): EventEntity {
        return EventEntity.Impl(
                event_id = generateEventId(eventId),
                room_id = roomId,
                type = "type",
                age = 0,
                age_local_ts = 0,
                content = "mycontent",
                prev_content = null,
                decryption_error_code = null,
                decryption_result_json = null,
                origin_server_ts = System.currentTimeMillis(),
                redacts = null,
                sender_id = null,
                state_key = null,
                unsigned_data = null,
                send_state = "Send"
        )
    }


}
