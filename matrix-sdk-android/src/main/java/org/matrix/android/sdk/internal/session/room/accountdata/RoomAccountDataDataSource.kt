/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.accountdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.asLiveData
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.mapper.AccountDataMapper
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.queryIn
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class RoomAccountDataDataSource @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        private val accountDataMapper: AccountDataMapper
) {

    fun getAccountDataEvent(roomId: String, type: String): RoomAccountDataEvent? {
        return getAccountDataEvents(roomId, setOf(type)).firstOrNull()
    }

    fun getLiveAccountDataEvent(roomId: String, type: String): LiveData<Optional<RoomAccountDataEvent>> {
        return Transformations.map(getLiveAccountDataEvents(roomId, setOf(type))) {
            it.firstOrNull().toOptional()
        }
    }

    /**
     * @param roomId the roomId to search for account data event. If null will check in every room.
     * @param types the types to filter. If empty will return all account data event in given room (or every room if roomId is null)
     *
     */
    fun getAccountDataEvents(roomId: String?, types: Set<String>): List<RoomAccountDataEvent> {
        val realm = realmInstance.getBlockingRealm()
        return buildRoomQuery(realm, roomId, types).find().flatMap {
            it.accountDataEvents(types)
        }
    }

    /**
     * @param roomId the roomId to search for account data event. If null will check in every room.
     * @param types the types to filter. If empty will return all account data event in the given room (or every room if roomId is null).
     *
     */
    fun getLiveAccountDataEvents(roomId: String?, types: Set<String>): LiveData<List<RoomAccountDataEvent>> {
        val liveRoomEntity = realmInstance.queryResults {
            buildRoomQuery(it, roomId, types)
        }.asLiveData()
        val resultLiveData = MediatorLiveData<List<RoomAccountDataEvent>>()
        resultLiveData.addSource(liveRoomEntity) { changeSet ->
            val mappedResult = changeSet.list.flatMap { it.accountDataEvents(types) }
            resultLiveData.postValue(mappedResult)
        }
        return resultLiveData
    }

    private fun buildRoomQuery(realm: TypedRealm, roomId: String?, types: Set<String>): RealmQuery<RoomEntity> {
        return realm.query(RoomEntity::class)
                .andIf(roomId != null) {
                    query("roomId == $0", roomId!!)
                }
                .query("accountData.@count > 0")
                .andIf(types.isNotEmpty()) {
                    queryIn("accountData.type", types)
                }
    }

    private fun RoomEntity.accountDataEvents(types: Set<String>): List<RoomAccountDataEvent> {
        return if (types.isNotEmpty()) {
            accountData.filter { types.contains(it.type) }
        } else {
            accountData
        }.map {
            accountDataMapper.map(roomId, it)
        }
    }
}
