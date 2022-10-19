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

package org.matrix.android.sdk.internal.database

import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import timber.log.Timber
import javax.inject.Inject

internal class EventInsertLiveObserver @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        coroutineDispatchers: MatrixCoroutineDispatchers,
        private val processors: Set<@JvmSuppressWildcards EventInsertLiveProcessor>
) : SessionLifecycleObserver {

    private val lock = Mutex()
    private val observerScope = CoroutineScope(Job() + coroutineDispatchers.io)

    override fun onSessionStopped(session: Session) {
        observerScope.cancel()
    }

    init {
        realmInstance.queryResults {
            it.query(EventInsertEntity::class, "canBeProcessed == true")
        }.onEach { resultChange ->
            // Use realmInstance scope as we want cancellation when cleared cached
            realmInstance.coroutineScope.processResultChange(resultChange)
        }.launchIn(observerScope)
    }

    private fun CoroutineScope.processResultChange(resultsChange: ResultsChange<EventInsertEntity>) = launch {
        onChange(resultsChange.list)
    }

    private suspend fun onChange(results: RealmResults<EventInsertEntity>) = coroutineScope {
        lock.withLock {
            if (results.isEmpty()) {
                return@coroutineScope
            }
            Timber.v("EventInsertEntity updated with ${results.size} results in db")
            realmInstance.write { ->
                results.forEach { eventInsert ->
                    deleteNullable(findLatest(eventInsert))
                    val eventId = eventInsert.eventId
                    val event = EventEntity.where(this, eventId).first().find()
                    if (event == null) {
                        Timber.v("Event $eventId not found")
                        return@forEach
                    }
                    val domainEvent = event.asDomain()
                    processors.filter {
                        it.shouldProcess(eventId, domainEvent.getClearType(), eventInsert.insertType)
                    }.forEach {
                        it.process(this, domainEvent)
                    }
                }
            }
            if (!isActive) {
                return@withLock
            }
            processors.forEach { it.onPostProcess() }
        }
    }
}
