/*
 * Copyright 2019 New Vector Ltd
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
 */

package im.vector.matrix.android.internal.network.parsing

import com.squareup.moshi.JsonReader
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity

internal class GetRoomMembersResponseHandler {

    companion object {
        private val NAMES = JsonReader.Options.of("event_id", "content", "prev_content", "origin_server_ts", "sender", "state_key")
    }

    internal fun handle(reader: JsonReader, roomEntity: RoomEntity, stateIndex: Int = Int.MIN_VALUE, isUnlinked: Boolean = false) {
        reader.readObject {
            reader.nextName()
            val eventEntity = EventEntity().apply {
                this.roomId = roomEntity.roomId
                this.stateIndex = stateIndex
                this.isUnlinked = isUnlinked
                this.sendState = SendState.SYNCED
                this.type = EventType.STATE_ROOM_MEMBER
            }
            reader.readArray {
                reader.readObject {
                    when
                        (reader.selectName(NAMES)) {
                        0    -> eventEntity.eventId = reader.nextString()
                        1    -> eventEntity.content = reader.readJsonValue()?.toString()
                        2    -> eventEntity.prevContent = reader.readJsonValue()?.toString()
                        3    -> eventEntity.originServerTs = reader.nextLong()
                        4    -> eventEntity.sender = reader.nextString()
                        5    -> eventEntity.stateKey = reader.nextString()
                        else -> reader.skipNameAndValue()
                    }
                }
                roomEntity.untimelinedStateEvents.add(eventEntity)
            }
        }
    }

}