/*
 * Copyright 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.send

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.database.helper.nextId
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.findAllInRoomWithSendStates
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.DefaultTimeline
import org.matrix.android.sdk.internal.util.awaitTransaction
import io.realm.Realm
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class LocalEchoRepository @Inject constructor(@SessionDatabase private val monarchy: Monarchy,
                                                       private val roomSummaryUpdater: RoomSummaryUpdater,
                                                       private val eventBus: EventBus,
                                                       private val timelineEventMapper: TimelineEventMapper) {

    fun createLocalEcho(event: Event) {
        val roomId = event.roomId ?: throw IllegalStateException("You should have set a roomId for your event")
        val senderId = event.senderId ?: throw IllegalStateException("You should have set a senderIf for your event")
        if (event.eventId == null) {
            throw IllegalStateException("You should have set an eventId for your event")
        }
        val timelineEventEntity = Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            val eventEntity = event.toEntity(roomId, SendState.UNSENT, System.currentTimeMillis())
            val roomMemberHelper = RoomMemberHelper(realm, roomId)
            val myUser = roomMemberHelper.getLastRoomMember(senderId)
            val localId = TimelineEventEntity.nextId(realm)
            TimelineEventEntity(localId).also {
                it.root = eventEntity
                it.eventId = event.eventId
                it.roomId = roomId
                it.senderName = myUser?.displayName
                it.senderAvatar = myUser?.avatarUrl
                it.isUniqueDisplayName = roomMemberHelper.isUniqueDisplayName(myUser?.displayName)
            }
        }
        val timelineEvent = timelineEventMapper.map(timelineEventEntity)
        eventBus.post(DefaultTimeline.OnLocalEchoCreated(roomId = roomId, timelineEvent = timelineEvent))
        monarchy.writeAsync { realm ->
            val eventInsertEntity = EventInsertEntity(event.eventId, event.type).apply {
                this.insertType = EventInsertType.LOCAL_ECHO
            }
            realm.insert(eventInsertEntity)
            val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst() ?: return@writeAsync
            roomEntity.sendingTimelineEvents.add(0, timelineEventEntity)
            roomSummaryUpdater.updateSendingInformation(realm, roomId)
        }
    }

    fun updateSendState(eventId: String, sendState: SendState) {
        Timber.v("Update local state of $eventId to ${sendState.name}")
        monarchy.writeAsync { realm ->
            val sendingEventEntity = EventEntity.where(realm, eventId).findFirst()
            if (sendingEventEntity != null) {
                if (sendState == SendState.SENT && sendingEventEntity.sendState == SendState.SYNCED) {
                    // If already synced, do not put as sent
                } else {
                    sendingEventEntity.sendState = sendState
                }
                roomSummaryUpdater.updateSendingInformation(realm, sendingEventEntity.roomId)
            }
        }
    }

    fun updateEncryptedEcho(eventId: String, encryptedContent: Content, mxEventDecryptionResult: MXEventDecryptionResult) {
        monarchy.writeAsync { realm ->
            val sendingEventEntity = EventEntity.where(realm, eventId).findFirst()
            if (sendingEventEntity != null) {
                sendingEventEntity.type = EventType.ENCRYPTED
                sendingEventEntity.content = ContentMapper.map(encryptedContent)
                sendingEventEntity.setDecryptionResult(mxEventDecryptionResult)
            }
        }
    }

    suspend fun deleteFailedEcho(roomId: String, localEcho: TimelineEvent) {
        monarchy.awaitTransaction { realm ->
            TimelineEventEntity.where(realm, roomId = roomId, eventId = localEcho.root.eventId ?: "").findFirst()?.deleteFromRealm()
            EventEntity.where(realm, eventId = localEcho.root.eventId ?: "").findFirst()?.deleteFromRealm()
            roomSummaryUpdater.updateSendingInformation(realm, roomId)
        }
    }

    suspend fun clearSendingQueue(roomId: String) {
        monarchy.awaitTransaction { realm ->
            TimelineEventEntity
                    .findAllInRoomWithSendStates(realm, roomId, SendState.IS_SENDING_STATES)
                    .forEach {
                        it.root?.sendState = SendState.UNSENT
                    }
            roomSummaryUpdater.updateSendingInformation(realm, roomId)
        }
    }

    suspend fun updateSendState(roomId: String, eventIds: List<String>, sendState: SendState) {
        monarchy.awaitTransaction { realm ->
            val timelineEvents = TimelineEventEntity.where(realm, roomId, eventIds).findAll()
            timelineEvents.forEach {
                it.root?.sendState = sendState
            }
            roomSummaryUpdater.updateSendingInformation(realm, roomId)
        }
    }

    fun getAllFailedEventsToResend(roomId: String): List<Event> {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            TimelineEventEntity
                    .findAllInRoomWithSendStates(realm, roomId, SendState.HAS_FAILED_STATES)
                    .sortedByDescending { it.displayIndex }
                    .mapNotNull { it.root?.asDomain() }
                    .filter { event ->
                        when (event.getClearType()) {
                            EventType.MESSAGE,
                            EventType.REDACTION,
                            EventType.REACTION -> {
                                val content = event.getClearContent().toModel<MessageContent>()
                                if (content != null) {
                                    when (content.msgType) {
                                        MessageType.MSGTYPE_EMOTE,
                                        MessageType.MSGTYPE_NOTICE,
                                        MessageType.MSGTYPE_LOCATION,
                                        MessageType.MSGTYPE_TEXT  -> {
                                            true
                                        }
                                        MessageType.MSGTYPE_FILE,
                                        MessageType.MSGTYPE_VIDEO,
                                        MessageType.MSGTYPE_IMAGE,
                                        MessageType.MSGTYPE_AUDIO -> {
                                            // need to resend the attachment
                                            false
                                        }
                                        else                      -> {
                                            Timber.e("Cannot resend message ${event.type} / ${content.msgType}")
                                            false
                                        }
                                    }
                                } else {
                                    Timber.e("Unsupported message to resend ${event.type}")
                                    false
                                }
                            }
                            else               -> {
                                Timber.e("Unsupported message to resend ${event.type}")
                                false
                            }
                        }
                    }
        }
    }
}
