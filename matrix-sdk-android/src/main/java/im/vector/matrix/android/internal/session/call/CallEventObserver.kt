/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.call

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.whereTypes
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.EventRelationsAggregationTask
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmConfiguration
import io.realm.RealmResults
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class CallEventObserver @Inject constructor(
        @SessionDatabase realmConfiguration: RealmConfiguration,
        @UserId private val userId: String,
        private val task: CallEventsObserverTask)  :
        RealmLiveEntityObserver<EventEntity>(realmConfiguration) {

    override val query = Monarchy.Query<EventEntity> {
        EventEntity.whereTypes(it, listOf(
                EventType.CALL_ANSWER,
                EventType.CALL_CANDIDATES,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.ENCRYPTED)
        )
    }

    override fun onChange(results: RealmResults<EventEntity>, changeSet: OrderedCollectionChangeSet) {
        Timber.v("EventRelationsAggregationUpdater called with ${changeSet.insertions.size} insertions")

        val insertedDomains = changeSet.insertions
                .asSequence()
                .mapNotNull { results[it]?.asDomain() }
                .toList()

        val params = CallEventsObserverTask.Params(
                insertedDomains,
                userId
        )
        observerScope.launch {
            task.execute(params)
        }
    }
}
