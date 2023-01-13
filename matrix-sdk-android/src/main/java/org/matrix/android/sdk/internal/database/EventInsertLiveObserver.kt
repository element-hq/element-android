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

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
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
        private val processors: Set<@JvmSuppressWildcards EventInsertLiveProcessor>,
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
                val eventsToProcess = ArrayList<EventInsertEntity>(results.size)
                val eventsToIgnore = ArrayList<EventInsertEntity>(results.size)

                Timber.v("EventInsertEntity updated with ${results.size} results in db")
                results.forEach {
                    // don't use copy from realm over there
                    val copiedEvent = EventInsertEntity(
                            eventId = it.eventId,
                            eventType = it.eventType
                    ).apply {
                        insertType = it.insertType
                    }

                    if (shouldProcess(it)) {
                        eventsToProcess.add(copiedEvent)
                    } else {
                        eventsToIgnore.add(copiedEvent)
                    }
                }

                awaitTransaction(realmConfiguration) { realm ->
                    Timber.v("##Transaction: There are ${eventsToProcess.size} events to process")

                    val idsToDeleteAfterProcess = ArrayList<String>()
                    val idsOfEncryptedEvents = ArrayList<String>()
                    val getAndTriageEvent: (EventInsertEntity) -> Event? = { eventInsert ->
                        val eventId = eventInsert.eventId
                        val event = getEvent(realm, eventId)
                        if (event?.getClearType() == EventType.ENCRYPTED) {
                            idsOfEncryptedEvents.add(eventId)
                        } else {
                            idsToDeleteAfterProcess.add(eventId)
                        }
                        event
                    }

                    eventsToProcess.forEach { eventInsert ->
                        val eventId = eventInsert.eventId
                        val event = getAndTriageEvent(eventInsert)

                        if (event != null && canProcessEvent(event)) {
                            processors.filter {
                                it.shouldProcess(eventId, event.getClearType(), eventInsert.insertType)
                            }.forEach {
                                it.process(realm, event)
                            }
                        } else {
                            Timber.v("Cannot process event with id $eventId")
                            return@forEach
                        }
                    }

                    eventsToIgnore.forEach { getAndTriageEvent(it) }

                    realm.where(EventInsertEntity::class.java)
                            .`in`(EventInsertEntityFields.EVENT_ID, idsToDeleteAfterProcess.toTypedArray())
                            .findAll()
                            .deleteAllFromRealm()

                    // make the encrypted events not processable: they will be processed again after decryption
                    realm.where(EventInsertEntity::class.java)
                            .`in`(EventInsertEntityFields.EVENT_ID, idsOfEncryptedEvents.toTypedArray())
                            .findAll()
                            .forEach { it.canBeProcessed = false }
                }
                processors.forEach { it.onPostProcess() }
            }
        }
    }

    private fun getEvent(realm: Realm, eventId: String): Event? {
        val event = EventEntity.where(realm, eventId).findFirst()
        if (event == null) {
            Timber.v("Event $eventId not found")
        }
        return event?.asDomain()
    }

    private fun canProcessEvent(event: Event): Boolean {
        // event should be either not encrypted or if encrypted it should contain relatesTo content
        return event.getClearType() != EventType.ENCRYPTED ||
                event.content.toModel<EncryptedEventContent>()?.relatesTo != null
    }

    private fun shouldProcess(eventInsertEntity: EventInsertEntity): Boolean {
        return processors.any {
            it.shouldProcess(eventInsertEntity.eventId, eventInsertEntity.eventType, eventInsertEntity.insertType)
        }
    }
}
