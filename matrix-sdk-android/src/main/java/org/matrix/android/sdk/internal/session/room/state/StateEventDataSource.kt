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

package org.matrix.android.sdk.internal.session.room.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.queryIn
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.QueryStringValueProcessor
import org.matrix.android.sdk.internal.util.mapOptional
import javax.inject.Inject

internal class StateEventDataSource @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        private val queryStringValueProcessor: QueryStringValueProcessor
) {

    fun getStateEvent(roomId: String, eventType: String, stateKey: QueryStateEventValue): Event? {
        val realm = realmInstance.getBlockingRealm()
        return buildStateEventQuery(realm, roomId, setOf(eventType), stateKey)
                .first()
                .find()
                ?.root
                ?.asDomain()
    }

    fun getStateEventLive(roomId: String, eventType: String, stateKey: QueryStateEventValue): LiveData<Optional<Event>> {
        return realmInstance.queryFirst {
            buildStateEventQuery(it, roomId, setOf(eventType), stateKey).first()
        }.mapOptional {
            it.root?.asDomain()
        }.asLiveData()
    }

    fun getStateEvents(roomId: String, eventTypes: Set<String>, stateKey: QueryStateEventValue): List<Event> {
        val realm = realmInstance.getBlockingRealm()
        return buildStateEventQuery(realm, roomId, eventTypes, stateKey)
                .find()
                .mapNotNull {
                    it.root?.asDomain()
                }
    }

    fun getStateEventsLive(roomId: String, eventTypes: Set<String>, stateKey: QueryStateEventValue): LiveData<List<Event>> {
        return realmInstance.queryList(this::map) { realm ->
            buildStateEventQuery(realm, roomId, eventTypes, stateKey)
        }.map {
            it.filterNotNull()
        }.asLiveData()
    }

    private fun map(stateEventEntity: CurrentStateEventEntity): Event? {
        return stateEventEntity.root?.asDomain()
    }

    private fun buildStateEventQuery(
            realm: TypedRealm,
            roomId: String,
            eventTypes: Set<String>,
            stateKey: QueryStateEventValue
    ): RealmQuery<CurrentStateEventEntity> {
        return with(queryStringValueProcessor) {
            realm.query(CurrentStateEventEntity::class)
                    .query("roomId == $0", roomId)
                    .andIf(eventTypes.isNotEmpty()) {
                        queryIn("type", eventTypes.toList())
                    }
                    // It's OK to cast stateKey as QueryStringValue
                    .process(CurrentStateEventEntityFields.STATE_KEY, stateKey as QueryStringValue)
        }
    }
}
