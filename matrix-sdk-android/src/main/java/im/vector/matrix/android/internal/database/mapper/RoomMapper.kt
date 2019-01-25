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

import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.session.room.DefaultRoom


internal object RoomMapper {


    fun map(roomEntity: RoomEntity): Room {
        return DefaultRoom(
                roomEntity.roomId,
                roomEntity.membership
        )
    }
}

internal fun RoomEntity.asDomain(): Room {
    return RoomMapper.map(this)
}