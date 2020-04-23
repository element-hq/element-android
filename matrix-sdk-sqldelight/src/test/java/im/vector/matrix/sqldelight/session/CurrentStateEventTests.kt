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
import org.amshove.kluent.shouldHaveSize
import org.junit.Test

class CurrentStateEventTests : SessionTests {

    private val roomId = "roomId"
    private val state_key = "state_key"
    private val state_type = "state_type"

    @Test
    fun test_all_operations() {
        val database = sessionDatabase()
        val insertCount = 100
        database.transaction {
            (0 until insertCount).forEach {
                val isStateEvent = it % 1 == 0
                val event = createEvent(it, isStateEvent)
                database.eventQueries.insert(event)
                if (isStateEvent) {
                    val stateEvent = CurrentStateEventEntity.Impl(event.event_id, roomId, state_key, state_type)
                    database.stateEventQueries.insertOrUpdate(stateEvent)
                }
            }
        }
        val lastStateEventId = generateEventId(insertCount - 1)
        database.stateEventQueries.getCurrentStateEvent(roomId = roomId, stateKey = state_key, type = state_type).executeAsList() shouldHaveSize 1
        val currentStateEvent = database.stateEventQueries.getCurrentStateEvent(roomId = roomId, stateKey = state_key, type = state_type).executeAsOne()
        currentStateEvent.event_id `should be equal to` lastStateEventId
        database.eventQueries.delete(lastStateEventId)
        database.stateEventQueries.getCurrentStateEvent(roomId = roomId, stateKey = state_key, type = state_type).executeAsList() shouldHaveSize 0
    }

    private fun generateEventId(id: Int) = "eventId_$id"

    private fun createEvent(eventId: Int, stateEvent: Boolean): EventEntity {
        val (type, stateKey) = if (stateEvent) {
            Pair(state_type, state_key)
        } else {
            Pair("type", null)
        }
        return EventEntity.Impl(
                event_id = generateEventId(eventId),
                room_id = roomId,
                type = type,
                age = 0,
                age_local_ts = 0,
                content = "mycontent",
                prev_content = null,
                decryption_error_code = null,
                decryption_result_json = null,
                origin_server_ts = System.currentTimeMillis(),
                redacts = null,
                sender_id = null,
                state_key = stateKey,
                unsigned_data = null,
                send_state = ""
        )
    }


}
