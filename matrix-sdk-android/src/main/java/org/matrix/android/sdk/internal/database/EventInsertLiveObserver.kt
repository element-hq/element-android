/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database

import com.zhuinden.monarchy.Monarchy
import io.realm.RealmConfiguration
import io.realm.RealmResults
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertEntity
import org.matrix.android.sdk.internal.database.model.EventInsertEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import timber.log.Timber
import javax.inject.Inject

internal class EventInsertLiveObserver @Inject constructor(
        @SessionDatabase realmConfiguration: RealmConfiguration,
        private val processors: Set<@JvmSuppressWildcards EventInsertLiveProcessor>
) :
        RealmLiveEntityObserver<EventInsertEntity>(realmConfiguration) {

    private val lock = Mutex()

    override val query = Monarchy.Query {
        it.where(EventInsertEntity::class.java).equalTo(EventInsertEntityFields.CAN_BE_PROCESSED, true)
    }

    override fun onChange(results: RealmResults<EventInsertEntity>) {
        observerScope.launch {
            lock.withLock {
                if (!results.isLoaded || results.isEmpty()) {
                    return@withLock
                }
                val idsToDeleteAfterProcess = ArrayList<String>()
                val filteredEvents = ArrayList<EventInsertEntity>(results.size)
                Timber.v("EventInsertEntity updated with ${results.size} results in db")
                results.forEach {
                    if (shouldProcess(it)) {
                        // don't use copy from realm over there
                        val copiedEvent = EventInsertEntity(
                                eventId = it.eventId,
                                eventType = it.eventType
                        ).apply {
                            insertType = it.insertType
                        }
                        filteredEvents.add(copiedEvent)
                    }
                    idsToDeleteAfterProcess.add(it.eventId)
                }
                awaitTransaction(realmConfiguration) { realm ->
                    Timber.v("##Transaction: There are ${filteredEvents.size} events to process ")
                    filteredEvents.forEach { eventInsert ->
                        val eventId = eventInsert.eventId
                        val event = EventEntity.where(realm, eventId).findFirst()
                        if (event == null) {
                            Timber.v("Event $eventId not found")
                            return@forEach
                        }
                        val domainEvent = event.asDomain()
                        processors.filter {
                            it.shouldProcess(eventId, domainEvent.getClearType(), eventInsert.insertType)
                        }.forEach {
                            it.process(realm, domainEvent)
                        }
                    }
                    realm.where(EventInsertEntity::class.java)
                            .`in`(EventInsertEntityFields.EVENT_ID, idsToDeleteAfterProcess.toTypedArray())
                            .findAll()
                            .deleteAllFromRealm()
                }
                processors.forEach { it.onPostProcess() }
            }
        }
    }

    private fun shouldProcess(eventInsertEntity: EventInsertEntity): Boolean {
        return processors.any {
            it.shouldProcess(eventInsertEntity.eventId, eventInsertEntity.eventType, eventInsertEntity.insertType)
        }
    }
}
