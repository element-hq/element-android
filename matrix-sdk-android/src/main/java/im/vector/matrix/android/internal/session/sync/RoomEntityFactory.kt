/*
 * Copyright 2019 New Vector Ltd
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
 *
 */

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.tag.RoomTagContent
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvent
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.read.FullyReadContent
import im.vector.matrix.android.internal.session.sync.model.RoomSync
import im.vector.matrix.android.internal.session.sync.model.RoomSyncAccountData
import im.vector.matrix.android.internal.session.sync.model.RoomSyncEphemeral
import im.vector.matrix.android.internal.session.user.UserEntityFactory
import io.realm.Realm
import io.realm.kotlin.createObject
import javax.inject.Inject

internal class RoomEntityFactory @Inject constructor(private val cryptoService: DefaultCryptoService,
                                                     private val readReceiptHandler: ReadReceiptHandler,
                                                     private val roomTagHandler: RoomTagHandler,
                                                     private val roomFullyReadHandler: RoomFullyReadHandler,
                                                     private val chunkEntityFactory: ChunkEntityFactory,
                                                     private val roomSummaryUpdater: RoomSummaryUpdater) {

    fun create(realm: Realm,
               roomId: String,
               roomSync: RoomSync,
               membership: Membership,
               isInitialSync: Boolean): RoomEntity {
        return if (isInitialSync) {
            initialSyncStrategy(realm, roomId, roomSync, membership)
        } else {
            incrementalSyncStrategy(realm, roomId, roomSync, membership)
        }
    }

    private fun initialSyncStrategy(realm: Realm, roomId: String, roomSync: RoomSync, membership: Membership): RoomEntity {
        val roomEntity = realm.createObject<RoomEntity>(roomId).apply {
            this.membership = membership
        }
        if (roomSync.ephemeral != null && roomSync.ephemeral.events.isNotEmpty()) {
            handleEphemeral(realm, roomId, roomSync.ephemeral, isInitialSync = true)
        }
        if (roomSync.accountData != null && roomSync.accountData.events.isNullOrEmpty().not()) {
            handleRoomAccountDataEvents(realm, roomId, roomSync.accountData)
        }
        // State events
        if (roomSync.state != null && roomSync.state.events.isNotEmpty()) {
            roomSync.state.events.forEach { event ->
                roomEntity.addStateEvent(event)
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                UserEntityFactory.createOrNull(event)?.also {
                    realm.insertOrUpdate(it)
                }
            }
        }
        // Timeline events
        if (roomSync.timeline != null && roomSync.timeline.events.isNotEmpty()) {
            val chunkEntity = chunkEntityFactory.create(
                    realm,
                    roomId,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    isInitialSync = true
            )
            roomEntity.chunks.add(chunkEntity)
        }
        roomSummaryUpdater.update(realm, roomId, Membership.JOIN, roomSync.summary, roomSync.unreadNotifications, updateMembers = true)
        return roomEntity
    }

    private fun incrementalSyncStrategy(realm: Realm, roomId: String, roomSync: RoomSync, membership: Membership): RoomEntity {
        if (roomSync.ephemeral != null && roomSync.ephemeral.events.isNotEmpty()) {
            handleEphemeral(realm, roomId, roomSync.ephemeral, false)
        }

        if (roomSync.accountData != null && roomSync.accountData.events.isNullOrEmpty().not()) {
            handleRoomAccountDataEvents(realm, roomId, roomSync.accountData)
        }

        val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                ?: realm.createObject(roomId)

        if (roomEntity.membership == Membership.INVITE) {
            roomEntity.chunks.deleteAllFromRealm()
        }
        roomEntity.membership = membership
        // State event

        if (roomSync.state != null && roomSync.state.events.isNotEmpty()) {
            val minStateIndex = roomEntity.untimelinedStateEvents.where().min(EventEntityFields.STATE_INDEX)?.toInt()
                    ?: Int.MIN_VALUE
            val untimelinedStateIndex = minStateIndex + 1
            roomSync.state.events.forEach { event ->
                roomEntity.addStateEvent(event, stateIndex = untimelinedStateIndex)
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                UserEntityFactory.createOrNull(event)?.also {
                    realm.insertOrUpdate(it)
                }
            }
        }
        if (roomSync.timeline != null && roomSync.timeline.events.isNotEmpty()) {
            val chunkEntity = chunkEntityFactory.create(
                    realm,
                    roomId,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    false
            )
            roomEntity.addOrUpdate(chunkEntity)
        }
        val hasRoomMember = roomSync.state?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null || roomSync.timeline?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null

        roomSummaryUpdater.update(realm, roomId, Membership.JOIN, roomSync.summary, roomSync.unreadNotifications, updateMembers = hasRoomMember)
        return roomEntity
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleEphemeral(realm: Realm,
                                roomId: String,
                                ephemeral: RoomSyncEphemeral,
                                isInitialSync: Boolean) {
        for (event in ephemeral.events) {
            if (event.type != EventType.RECEIPT) continue
            val readReceiptContent = event.content as? ReadReceiptContent ?: continue
            readReceiptHandler.handle(realm, roomId, readReceiptContent, isInitialSync)
        }
    }

    private fun handleRoomAccountDataEvents(realm: Realm, roomId: String, accountData: RoomSyncAccountData) {
        for (event in accountData.events) {
            val eventType = event.getClearType()
            if (eventType == EventType.TAG) {
                val content = event.getClearContent().toModel<RoomTagContent>()
                roomTagHandler.handle(realm, roomId, content)
            } else if (eventType == EventType.FULLY_READ) {
                val content = event.getClearContent().toModel<FullyReadContent>()
                roomFullyReadHandler.handle(realm, roomId, content)
            }
        }
    }


}
