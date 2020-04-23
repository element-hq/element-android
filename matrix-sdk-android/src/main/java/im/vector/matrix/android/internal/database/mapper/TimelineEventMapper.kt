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

package im.vector.matrix.android.internal.database.mapper

import com.squareup.sqldelight.db.SqlCursor
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

internal class TimelineEventMapper @Inject constructor() {

    fun map(cursor: SqlCursor): TimelineEvent = map(
            cursor.getLong(0)!!,
            cursor.getLong(1)!!,
            cursor.getLong(2)!!.toInt(),
            cursor.getString(3),
            cursor.getString(4),
            cursor.getLong(5)!! == 1L,
            cursor.getString(6)!!,
            cursor.getString(7)!!,
            cursor.getString(8),
            cursor.getString(9),
            cursor.getString(10),
            cursor.getString(11)!!,
            cursor.getString(12)!!,
            cursor.getLong(13),
            cursor.getString(14),
            cursor.getString(15),
            cursor.getString(16),
            cursor.getLong(17),
            cursor.getLong(18),
            cursor.getString(19),
            cursor.getString(20)
    )

    fun map(local_id: Long,
            chunk_id: Long,
            display_index: Int,
            sender_name: String?,
            sender_avatar: String?,
            is_unique_display_name: Boolean,
            event_id: String,
            room_id: String,
            content: String?,
            prev_content: String?,
            state_key: String?,
            send_state: String,
            type: String,
            origin_server_ts: Long?,
            sender_id: String?,
            unsigned_data: String?,
            redacts: String?,
            age: Long?,
            age_local_ts: Long?,
            decryption_result_json: String?,
            decryption_error_code: String?): TimelineEvent {

        val event = Event(
                type = type,
                roomId = room_id,
                eventId = event_id,
                content = ContentMapper.map(content),
                prevContent = ContentMapper.map(prev_content),
                originServerTs = origin_server_ts,
                senderId = sender_id,
                redacts = redacts,
                stateKey = state_key,
                unsignedData = UnsignedDataMapper.mapFromString(unsigned_data)
        ).also {
            it.ageLocalTs = age_local_ts
            it.sendState = SendState.valueOf(send_state)
            it.setDecryptionValues(decryption_result_json, decryption_error_code)
        }
        return TimelineEvent(
                root = event,
                eventId = event_id,
                annotations = null,
                displayIndex = display_index,
                isUniqueDisplayName = is_unique_display_name,
                localId = local_id,
                readReceipts = emptyList(),
                senderAvatar = sender_avatar,
                senderName = sender_name
        )
    }

}
