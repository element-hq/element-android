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

package im.vector.matrix.android.internal.database.model

import im.vector.matrix.android.api.session.room.send.SendState
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.*

internal open class EventEntity(@PrimaryKey var localId: String = UUID.randomUUID().toString(),
                                @Index var eventId: String = "",
                                var roomId: String = "",
                                @Index var type: String = "",
                                var content: String? = null,
                                var prevContent: String? = null,
                                @Index var stateKey: String? = null,
                                var originServerTs: Long? = null,
                                @Index var sender: String? = null,
                                var age: Long? = 0,
                                var unsignedData: String? = null,
                                var redacts: String? = null,
                                @Index var stateIndex: Int = 0,
                                @Index var displayIndex: Int = 0,
                                @Index var isUnlinked: Boolean = false
) : RealmObject() {

    enum class LinkFilterMode {
        LINKED_ONLY,
        UNLINKED_ONLY,
        BOTH
    }

    private var sendStateStr: String = SendState.UNKNOWN.name

    var sendState: SendState
        get() {
            return SendState.valueOf(sendStateStr)
        }
        set(value) {
            sendStateStr = value.name
        }


    companion object

    @LinkingObjects("events")
    val chunk: RealmResults<ChunkEntity>? = null

    @LinkingObjects("untimelinedStateEvents")
    val room: RealmResults<RoomEntity>? = null

}