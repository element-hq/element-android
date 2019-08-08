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

package im.vector.matrix.android.internal.session.user.accountdata

import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.getDirectRooms
import im.vector.matrix.android.internal.di.SessionDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

internal class DirectChatsHelper @Inject constructor(@SessionDatabase
                                                     private val realmConfiguration: RealmConfiguration) {

    /**
     * @return a map of userId <-> list of roomId
     */
    fun getLocalUserAccount(filterRoomId: String? = null): MutableMap<String, MutableList<String>> {
        return Realm.getInstance(realmConfiguration).use { realm ->
            val currentDirectRooms = RoomSummaryEntity.getDirectRooms(realm)
            val directChatsMap = mutableMapOf<String, MutableList<String>>()
            for (directRoom in currentDirectRooms) {
                if (directRoom.roomId == filterRoomId) continue
                val directUserId = directRoom.directUserId ?: continue
                directChatsMap
                        .getOrPut(directUserId, { arrayListOf() })
                        .apply {
                            add(directRoom.roomId)
                        }
            }
            directChatsMap
        }
    }


}