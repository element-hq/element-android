/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.abs

private const val INVITE_VALIDITY_TIME_WINDOW_MILLIS = 10 * 60_000

@SessionScope
internal class UnRequestedForwardManager @Inject constructor(
        private val deviceListManager: DeviceListManager,
) {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sequencer = SemaphoreCoroutineSequencer()

    // For now only in memory storage. Maybe we should persist? in case of gappy sync and long catchups?
    private val forwardedKeysPerRoom = mutableMapOf<String, MutableMap<String, MutableList<ForwardInfo>>>()

    data class InviteInfo(
            val roomId: String,
            val fromMxId: String,
            val timestamp: Long
    )

    data class ForwardInfo(
            val event: Event,
            val timestamp: Long
    )

    // roomId, local timestamp of invite
    private val recentInvites = mutableListOf<InviteInfo>()

    fun close() {
        try {
            scope.cancel("User Terminate")
        } catch (failure: Throwable) {
            Timber.w(failure, "Failed to shutDown UnrequestedForwardManager")
        }
    }

    fun onInviteReceived(roomId: String, fromUserId: String, localTimeStamp: Long) {
        Timber.w("Invite received in room:$roomId from:$fromUserId at $localTimeStamp")
        scope.launch {
            sequencer.post {
                if (!recentInvites.any { it.roomId == roomId && it.fromMxId == fromUserId }) {
                    recentInvites.add(
                            InviteInfo(
                                    roomId,
                                    fromUserId,
                                    localTimeStamp
                            )
                    )
                }
            }
        }
    }

    fun onUnRequestedKeyForward(roomId: String, event: Event, localTimeStamp: Long) {
        Timber.w("Received unrequested forward in room:$roomId from:${event.senderId} at $localTimeStamp")
        scope.launch {
            sequencer.post {
                val claimSenderId = event.senderId.orEmpty()
                val senderKey = event.getSenderKey()
                // we might want to download keys, as this user might not be known yet, cache is ok
                val ownerMxId =
                        tryOrNull {
                            deviceListManager.downloadKeys(listOf(claimSenderId), false)
                                    .map[claimSenderId]
                                    ?.values
                                    ?.firstOrNull { it.identityKey() == senderKey }
                                    ?.userId
                        }
                // Not sure what to do if the device has been deleted? I can't proove the mxid
                if (ownerMxId == null || claimSenderId != ownerMxId) {
                    Timber.w("Mismatch senderId between event and olm owner")
                    return@post
                }

                forwardedKeysPerRoom
                        .getOrPut(roomId) { mutableMapOf() }
                        .getOrPut(ownerMxId) { mutableListOf() }
                        .add(ForwardInfo(event, localTimeStamp))
            }
        }
    }

    fun postSyncProcessParkedKeysIfNeeded(currentTimestamp: Long, handleForwards: suspend (List<Event>) -> Unit) {
        scope.launch {
            sequencer.post {
                // Prune outdated invites
                recentInvites.removeAll { currentTimestamp - it.timestamp > INVITE_VALIDITY_TIME_WINDOW_MILLIS }
                val cleanUpEvents = mutableListOf<Pair<String, String>>()
                forwardedKeysPerRoom.forEach { (roomId, senderIdToForwardMap) ->
                    senderIdToForwardMap.forEach { (senderId, eventList) ->
                        // is there a matching invite in a valid timewindow?
                        val matchingInvite = recentInvites.firstOrNull { it.fromMxId == senderId && it.roomId == roomId }
                        if (matchingInvite != null) {
                            Timber.v("match  for room:$roomId from sender:$senderId -> count =${eventList.size}")

                            eventList.filter {
                                abs(matchingInvite.timestamp - it.timestamp) <= INVITE_VALIDITY_TIME_WINDOW_MILLIS
                            }.map {
                                it.event
                            }.takeIf { it.isNotEmpty() }?.let {
                                Timber.w("Re-processing forwarded_room_key_event that was not requested after invite")
                                scope.launch {
                                    handleForwards.invoke(it)
                                }
                            }
                            cleanUpEvents.add(roomId to senderId)
                        }
                    }
                }

                cleanUpEvents.forEach { roomIdToSenderPair ->
                    forwardedKeysPerRoom[roomIdToSenderPair.first]?.get(roomIdToSenderPair.second)?.clear()
                }
            }
        }
    }
}
