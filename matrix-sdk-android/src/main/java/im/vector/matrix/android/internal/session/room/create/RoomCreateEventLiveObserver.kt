/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.create

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.VersioningState
import im.vector.matrix.android.api.session.room.model.create.RoomCreateContent
import im.vector.matrix.android.api.session.room.model.tombstone.RoomTombstoneContent
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.types
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import javax.inject.Inject

internal class RoomCreateEventLiveObserver @Inject constructor(@SessionDatabase
                                                               realmConfiguration: RealmConfiguration)
    : RealmLiveEntityObserver<EventEntity>(realmConfiguration) {

    override val query = Monarchy.Query<EventEntity> {
        EventEntity.types(it, listOf(EventType.STATE_ROOM_CREATE))
    }

    override fun onChange(results: RealmResults<EventEntity>, changeSet: OrderedCollectionChangeSet) {
        changeSet.insertions
                .asSequence()
                .mapNotNull {
                    results[it]?.asDomain()
                }
                .toList()
                .also {
                    handleRoomCreateEvents(it)
                }
    }

    private fun handleRoomCreateEvents(createEvents: List<Event>) = Realm.getInstance(realmConfiguration).use {
        it.executeTransactionAsync { realm ->
            for (event in createEvents) {
                val createRoomContent = event.getClearContent().toModel<RoomCreateContent>()
                val predecessorRoomId = createRoomContent?.predecessor?.roomId ?: continue

                val predecessorRoomSummary = RoomSummaryEntity.where(realm, predecessorRoomId).findFirst()
                        ?: RoomSummaryEntity(predecessorRoomId)
                predecessorRoomSummary.versioningState = VersioningState.UPGRADED_ROOM_JOINED
                realm.insertOrUpdate(predecessorRoomSummary)

            }
        }
    }

}