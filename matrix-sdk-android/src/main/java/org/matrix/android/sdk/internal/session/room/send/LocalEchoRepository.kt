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

package org.matrix.android.sdk.internal.session.room.send

import io.realm.kotlin.MutableRealm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.RealmInstance
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
import org.matrix.android.sdk.internal.session.room.timeline.TimelineInput
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

internal class LocalEchoRepository @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        private val roomSummaryUpdater: RoomSummaryUpdater,
        private val timelineInput: TimelineInput,
        private val timelineEventMapper: TimelineEventMapper,
        private val clock: Clock,
) {

    fun createLocalEcho(event: Event) {
        val roomId = event.roomId ?: throw IllegalStateException("You should have set a roomId for your event")
        val senderId = event.senderId ?: throw IllegalStateException("You should have set a senderId for your event")
        event.eventId ?: throw IllegalStateException("You should have set an eventId for your event")
        event.type ?: throw IllegalStateException("You should have set a type for your event")

        val realm = realmInstance.getBlockingRealm()
        val eventEntity = event.toEntity(roomId, SendState.UNSENT, clock.epochMillis())
        val roomMemberHelper = RoomMemberHelper(realm, roomId)
        val myUser = roomMemberHelper.getLastRoomMember(senderId)
        val localId = UUID.randomUUID().mostSignificantBits
        val timelineEventEntity = TimelineEventEntity().also {
            it.localId = localId
            it.root = eventEntity
            it.eventId = event.eventId
            it.roomId = roomId
            it.senderName = myUser?.displayName
            it.senderAvatar = myUser?.avatarUrl
            it.isUniqueDisplayName = roomMemberHelper.isUniqueDisplayName(myUser?.displayName)
        }
        val timelineEvent = timelineEventMapper.map(timelineEventEntity)
        timelineInput.onLocalEchoCreated(roomId = roomId, timelineEvent = timelineEvent)
        realmInstance.asyncWrite {
            val eventInsertEntity = EventInsertEntity().apply {
                this.eventId = event.eventId
                this.insertType = EventInsertType.INCREMENTAL_SYNC
                this.canBeProcessed = true
                this.insertType = EventInsertType.LOCAL_ECHO
            }
            copyToRealm(eventInsertEntity)
            val roomEntity = RoomEntity.where(this, roomId = roomId).first().find() ?: return@asyncWrite
            roomEntity.sendingTimelineEvents.add(0, timelineEventEntity)
            roomSummaryUpdater.updateSendingInformation(this, roomId)
        }
    }

    fun updateSendState(eventId: String, roomId: String?, sendState: SendState, sendStateDetails: String? = null) {
        Timber.v("## SendEvent: [${clock.epochMillis()}] Update local state of $eventId to ${sendState.name}")
        timelineInput.onLocalEchoUpdated(roomId = roomId ?: "", eventId = eventId, sendState = sendState)
        updateEchoAsync(eventId) { realm, sendingEventEntity ->
            if (sendState == SendState.SENT && sendingEventEntity.sendState == SendState.SYNCED) {
                // If already synced, do not put as sent
            } else {
                sendingEventEntity.sendState = sendState
            }
            sendingEventEntity.sendStateDetails = sendStateDetails
            roomSummaryUpdater.updateSendingInformation(realm, sendingEventEntity.roomId)
        }
    }

    suspend fun updateEcho(eventId: String, block: (realm: MutableRealm, eventEntity: EventEntity) -> Unit) {
        realmInstance.write {
            val sendingEventEntity = EventEntity.where(this, eventId).first().find()
            if (sendingEventEntity != null) {
                block(this, sendingEventEntity)
            }
        }
    }

    fun updateEchoAsync(eventId: String, block: (realm: MutableRealm, eventEntity: EventEntity) -> Unit) {
        realmInstance.asyncWrite {
            val sendingEventEntity = EventEntity.where(this, eventId).first().find()
            if (sendingEventEntity != null) {
                block(this, sendingEventEntity)
            }
        }
    }

    suspend fun getUpToDateEcho(eventId: String): Event? {
        // We are using awaitTransaction here to make sure this executes after other transactions
        return realmInstance.write {
            EventEntity.where(this, eventId).first().find()?.asDomain(castJsonNumbers = true)
        }
    }

    suspend fun deleteFailedEcho(roomId: String, localEcho: TimelineEvent) {
        deleteFailedEcho(roomId, localEcho.eventId)
    }

    suspend fun deleteFailedEcho(roomId: String, eventId: String?) {
        realmInstance.write {
            TimelineEventEntity.where(this, roomId = roomId, eventId = eventId ?: "").first().find()?.also {
                delete(it)
            }
            EventEntity.where(this, eventId = eventId ?: "").first().find()?.also {
                delete(it)
            }
            roomSummaryUpdater.updateSendingInformation(this, roomId)
        }
    }

    fun deleteFailedEchoAsync(roomId: String, eventId: String?) {
        realmInstance.blockingWrite {
            TimelineEventEntity.where(this, roomId = roomId, eventId = eventId ?: "").first().find()?.also {
                delete(it)
            }
            EventEntity.where(this, eventId = eventId ?: "").first().find()?.also {
                delete(it)
            }
            roomSummaryUpdater.updateSendingInformation(this, roomId)
        }
    }

    suspend fun clearSendingQueue(roomId: String) {
        realmInstance.write {
            TimelineEventEntity
                    .findAllInRoomWithSendStates(this, roomId, SendState.IS_SENDING_STATES)
                    .forEach {
                        it.root?.sendState = SendState.UNSENT
                    }
            roomSummaryUpdater.updateSendingInformation(this, roomId)
        }
    }

    suspend fun updateSendState(roomId: String, eventIds: List<String>, sendState: SendState) {
        realmInstance.write {
            val timelineEvents = TimelineEventEntity.where(this, roomId, eventIds).find()
            timelineEvents.forEach {
                it.root?.sendState = sendState
                it.root?.sendStateDetails = null
            }
            roomSummaryUpdater.updateSendingInformation(this, roomId)
        }
    }

    fun getAllFailedEventsToResend(roomId: String): List<TimelineEvent> {
        return getAllEventsWithStates(roomId, SendState.HAS_FAILED_STATES)
    }

    fun getAllEventsWithStates(roomId: String, states: List<SendState>): List<TimelineEvent> {
        val realm = realmInstance.getBlockingRealm()
        return TimelineEventEntity
                .findAllInRoomWithSendStates(realm, roomId, states)
                .sortedByDescending { it.displayIndex }
                .map { it.let { timelineEventMapper.map(it) } }
                .filter { event ->
                    when (event.root.getClearType()) {
                        EventType.MESSAGE,
                        EventType.REDACTION,
                        EventType.REACTION -> {
                            val content = event.root.getClearContent().toModel<MessageContent>()
                            if (content != null) {
                                when (content.msgType) {
                                    MessageType.MSGTYPE_EMOTE,
                                    MessageType.MSGTYPE_NOTICE,
                                    MessageType.MSGTYPE_LOCATION,
                                    MessageType.MSGTYPE_TEXT,
                                    MessageType.MSGTYPE_FILE,
                                    MessageType.MSGTYPE_VIDEO,
                                    MessageType.MSGTYPE_IMAGE,
                                    MessageType.MSGTYPE_AUDIO -> {
                                        // need to resend the attachment
                                        true
                                    }
                                    else -> {
                                        Timber.e("Cannot resend message ${event.root.getClearType()} / ${content.msgType}")
                                        false
                                    }
                                }
                            } else {
                                Timber.e("Unsupported message to resend ${event.root.getClearType()}")
                                false
                            }
                        }
                        else -> {
                            Timber.e("Unsupported message to resend ${event.root.getClearType()}")
                            false
                        }
                    }
                }
    }

    /**
     * Returns the latest known thread event message, or the rootThreadEventId if no other event found.
     */
    fun getLatestThreadEvent(rootThreadEventId: String): String {
        val realm = realmInstance.getBlockingRealm()
        return EventEntity.where(realm, eventId = rootThreadEventId)
                .first()
                .find()
                ?.threadSummaryLatestMessage?.eventId ?: rootThreadEventId
    }
}
