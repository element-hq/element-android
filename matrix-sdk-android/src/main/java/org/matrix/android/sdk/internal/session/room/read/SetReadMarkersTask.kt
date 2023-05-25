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

package org.matrix.android.sdk.internal.session.room.read

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.isEventRead
import org.matrix.android.sdk.internal.database.query.isReadMarkerMoreRecent
import org.matrix.android.sdk.internal.database.query.latestEvent
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.sync.handler.room.ReadReceiptHandler
import org.matrix.android.sdk.internal.session.sync.handler.room.RoomFullyReadHandler
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.set

internal interface SetReadMarkersTask : Task<SetReadMarkersTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val fullyReadEventId: String? = null,
            val readReceiptEventId: String? = null,
            val readReceiptThreadId: String? = null,
            val forceReadReceipt: Boolean = false,
            val forceReadMarker: Boolean = false,
    )
}

private const val READ_MARKER = "m.fully_read"
private const val READ_RECEIPT = "m.read"

internal class DefaultSetReadMarkersTask @Inject constructor(
        private val roomAPI: RoomAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val roomFullyReadHandler: RoomFullyReadHandler,
        private val readReceiptHandler: ReadReceiptHandler,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val clock: Clock,
        private val homeServerCapabilitiesService: HomeServerCapabilitiesService,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
) : SetReadMarkersTask {

    override suspend fun execute(params: SetReadMarkersTask.Params) = withContext(coroutineDispatchers.io) {
        val markers = mutableMapOf<String, String>()
        Timber.v("Execute set read marker with params: $params")
        val latestSyncedEventId = latestSyncedEventId(params.roomId)
        val readReceiptThreadId = params.readReceiptThreadId
        val fullyReadEventId = if (params.forceReadMarker) {
            latestSyncedEventId
        } else {
            params.fullyReadEventId
        }
        val readReceiptEventId = if (params.forceReadReceipt) {
            latestSyncedEventId
        } else {
            params.readReceiptEventId
        }

        if (fullyReadEventId != null && !isReadMarkerMoreRecent(monarchy.realmConfiguration, params.roomId, fullyReadEventId)) {
            if (LocalEcho.isLocalEchoId(fullyReadEventId)) {
                Timber.w("Can't set read marker for local event $fullyReadEventId")
            } else {
                markers[READ_MARKER] = fullyReadEventId
            }
        }

        val shouldCheckIfReadInEventsThread = readReceiptThreadId != null &&
                homeServerCapabilitiesService.getHomeServerCapabilities().canUseThreadReadReceiptsAndNotifications

        if (readReceiptEventId != null &&
                !isEventRead(monarchy.realmConfiguration, userId, params.roomId, readReceiptEventId, shouldCheckIfReadInEventsThread)) {
            if (LocalEcho.isLocalEchoId(readReceiptEventId)) {
                Timber.w("Can't set read receipt for local event $readReceiptEventId")
            } else {
                markers[READ_RECEIPT] = readReceiptEventId
            }
        }

        val shouldUpdateRoomSummary = readReceiptEventId != null && readReceiptEventId == latestSyncedEventId
        if (markers.isNotEmpty() || shouldUpdateRoomSummary) {
            updateDatabase(params.roomId, readReceiptThreadId, markers, shouldUpdateRoomSummary)
        }
        if (markers.isNotEmpty()) {
            executeRequest(
                    globalErrorReceiver,
                    canRetry = true
            ) {
                if (markers[READ_MARKER] == null) {
                    if (readReceiptEventId != null) {
                        val readBody = ReadBody(threadId = params.readReceiptThreadId)
                        roomAPI.sendReceipt(params.roomId, READ_RECEIPT, readReceiptEventId, readBody)
                    }
                } else {
                    // "m.fully_read" value is mandatory to make this call
                    roomAPI.sendReadMarker(params.roomId, markers)
                }
            }
        }
    }

    private fun latestSyncedEventId(roomId: String): String? =
            Realm.getInstance(monarchy.realmConfiguration).use { realm ->
                TimelineEventEntity.latestEvent(realm, roomId = roomId, includesSending = false)?.eventId
            }

    private suspend fun updateDatabase(roomId: String, readReceiptThreadId: String?, markers: Map<String, String>, shouldUpdateRoomSummary: Boolean) {
        monarchy.awaitTransaction { realm ->
            val readMarkerId = markers[READ_MARKER]
            val readReceiptId = markers[READ_RECEIPT]
            if (readMarkerId != null) {
                roomFullyReadHandler.handle(realm, roomId, FullyReadContent(readMarkerId))
            }
            if (readReceiptId != null) {
                val readReceiptContent = ReadReceiptHandler.createContent(userId, readReceiptId, readReceiptThreadId, clock.epochMillis())
                readReceiptHandler.handle(realm, roomId, readReceiptContent, false, null)
            }
            if (shouldUpdateRoomSummary) {
                val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                        ?: return@awaitTransaction
                roomSummary.notificationCount = 0
                roomSummary.highlightCount = 0
                roomSummary.hasUnreadMessages = false
            }
        }
    }
}
