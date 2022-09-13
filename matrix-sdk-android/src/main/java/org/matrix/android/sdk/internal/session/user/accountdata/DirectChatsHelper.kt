/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.user.accountdata

import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.getDirectRooms
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.sync.model.accountdata.DirectMessagesContent
import javax.inject.Inject

internal class DirectChatsHelper @Inject constructor(
        @SessionDatabase private val realmConfiguration: RealmConfiguration
) {

    /**
     * @return a map of userId <-> list of roomId
     */
    fun getLocalDirectMessages(filterRoomId: String? = null): DirectMessagesContent {
        return Realm.getInstance(realmConfiguration).use { realm ->
            // Makes sure we have the latest realm updates, this is important as we sent this information to the server.
            realm.refresh()
            RoomSummaryEntity.getDirectRooms(realm)
                    .asSequence()
                    .filter { it.roomId != filterRoomId && it.directUserId != null && it.membership.isActive() }
                    .groupByTo(mutableMapOf(), { it.directUserId!! }, { it.roomId })
        }
    }
}
