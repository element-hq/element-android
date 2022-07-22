/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import okhttp3.internal.closeQuietly
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.OutgoingRoomKeyRequestState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntityFields
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import org.matrix.android.sdk.internal.util.createBackgroundHandler
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val loggerTag = LoggerTag("KeyGossipStatusManager", LoggerTag.CRYPTO)

/**
 * Used by DefaultTimeline to decorates timeline events with some crypto database related information.
 * The controller will then be able to update the display of events.
 */
internal class CryptoInfoEventDecorator(
        private val realmConfiguration: RealmConfiguration,
        private val roomId: String,
        private val clock: Clock,
        private val rebuildListener: RebuildListener,
        private val onEventsUpdated: (Boolean) -> Unit,
) : RealmChangeListener<RealmResults<OutgoingKeyRequestEntity>> {

    companion object {
        val BACKGROUND_HANDLER = createBackgroundHandler("DefaultTimeline_Thread")
        const val MAX_AGE_FOR_REQUEST_TO_BE_IDLE = 60_000L
    }

    private val dispatcher = BACKGROUND_HANDLER.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sequencer = SemaphoreCoroutineSequencer()

    private var outgoingRequestList: RealmResults<OutgoingKeyRequestEntity>? = null

    private var decoratedEvents = mutableMapOf<String, MutableList<String>>()

    private var activeOutgoingRequestsForSession = emptyList<String>()
    private var realmInstance = AtomicReference<Realm>()
    private val isStarted = AtomicBoolean(false)

    private var tickerTimer: Timer? = null

    private var isE2EE = false

    fun getActiveRequestForSession(): List<String> {
        return synchronized(this) {
            activeOutgoingRequestsForSession.toList()
        }
    }

    fun start() {
        scope.launch {
            sequencer.post {
                if (isStarted.compareAndSet(false, true)) {
                    realmInstance.set(Realm.getInstance(realmConfiguration))

                    realmInstance.get().where<CryptoRoomEntity>()
                            .equalTo(CryptoRoomEntityFields.ROOM_ID, roomId)
                            .findFirst()?.let {
                                isE2EE = it.wasEncryptedOnce ?: false
                            }

                    val now = clock.epochMillis()
                    // we can limit the query to the most recent request at the time of creation of the CryptoInfoEventDecorator
                    val createdAfter = now - MAX_AGE_FOR_REQUEST_TO_BE_IDLE
                    outgoingRequestList = realmInstance.get().where<OutgoingKeyRequestEntity>()
                            .equalTo(OutgoingKeyRequestEntityFields.ROOM_ID, roomId)
                            .`in`(OutgoingKeyRequestEntityFields.REQUEST_STATE_STR,
                                    listOf(
                                            OutgoingRoomKeyRequestState.UNSENT,
                                            OutgoingRoomKeyRequestState.SENT,
                                            OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND,
                                            OutgoingRoomKeyRequestState.SENT_THEN_CANCELED,
                                    )
                                            .map { it.name }
                                            .toTypedArray()
                            )
                            .greaterThan(OutgoingKeyRequestEntityFields.CREATION_TIME_STAMP, createdAfter)
                            .findAllAsync().also {
                                it?.addChangeListener(this@CryptoInfoEventDecorator)
                            }
                }
            }
        }
    }

    fun decorateTimelineEvent(event: TimelineEvent): TimelineEvent {
        if (!isE2EE) return event
        if (event.root.getClearType() != EventType.ENCRYPTED) return event
        val megolmSessionId = event.root.content?.get("session_id") as? String ?: return event
        // we might want to remember the built events per megolm session
        synchronized(this) {
            decoratedEvents.getOrPut(megolmSessionId) { mutableListOf() }.add(event.eventId)
        }
        val hasActiveRequests = getActiveRequestForSession().contains(megolmSessionId)
        return if (hasActiveRequests) {
            event.copy(
                    hasActiveRequestForKeys = hasActiveRequests
            )
        } else event
    }

    fun stop() {
        scope.coroutineContext.cancelChildren()
        scope.launch {
            sequencer.post {
                if (isStarted.compareAndSet(true, false)) {
                    outgoingRequestList?.removeAllChangeListeners()
                    decoratedEvents.clear()
                    outgoingRequestList = null
                    realmInstance.get().closeQuietly()
                    tickerTimer?.cancel()
                    tickerTimer = null
                }
            }
        }
    }

    override fun onChange(results: RealmResults<OutgoingKeyRequestEntity>) {
        if (!results.isLoaded || results.isEmpty()) {
            return
        }
        Timber.tag(loggerTag.value).v("OutgoingKeyRequests data changes")
        val now = clock.epochMillis()
        val newList = mutableListOf<String>()
        // we consider a request active for 1mn?
        results.forEach {
            if (!it.isValid) return@forEach
            val creationTs = it.creationTimeStamp ?: return@forEach
            val megolmSessionId = it.megolmSessionId ?: return@forEach
            val isActive = when (it.requestState) {
                OutgoingRoomKeyRequestState.UNSENT -> true
                OutgoingRoomKeyRequestState.SENT -> true
                OutgoingRoomKeyRequestState.SENT_THEN_CANCELED -> false
                OutgoingRoomKeyRequestState.CANCELLATION_PENDING -> false
                OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND -> true
            }
            val requestAge = now - creationTs

            if (isActive && requestAge < MAX_AGE_FOR_REQUEST_TO_BE_IDLE) {
                // we consider as active
                newList.add(megolmSessionId)
                Timber.tag(loggerTag.value).v("Adding active request for megolmSessionId $megolmSessionId age:$requestAge")
            } else {
                Timber.tag(loggerTag.value).v("Ignoring inactive request for megolmSessionId $megolmSessionId age:$requestAge")
            }
        }

        val sessionsToRebuild = mutableSetOf<String>()
        val builtMap = mutableMapOf<String, List<String>>()
        val oldActive = getActiveRequestForSession()
        synchronized(this) {
            sessionsToRebuild.addAll(oldActive)
            sessionsToRebuild.addAll(newList)
            activeOutgoingRequestsForSession = newList
            builtMap.putAll(decoratedEvents)
        }
        sessionsToRebuild
                .flatMap { builtMap[it] ?: emptyList() }
                .forEach { eventId ->
                    rebuildListener.rebuildEvent(eventId) {
                        decorateTimelineEvent(it)
                    }
                }

        onEventsUpdated(true)
        // do we need to schedule a ticker update next?
        if (newList.isNotEmpty()) {
            // yes we have active ones, we should refresh
            tickerTimer?.cancel()
            tickerTimer = Timer()
            tickerTimer?.schedule(
                    object : TimerTask() {
                        override fun run() {
                            Timber.tag(loggerTag.value).v("OutgoingKeyRequests ticker")
                            scope.launch {
                                outgoingRequestList?.let {
                                    onChange(it)
                                }
                            }
                        }
                    },
                    MAX_AGE_FOR_REQUEST_TO_BE_IDLE
            )
        }
    }
}
