/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.create

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.createObject
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.sync.model.RoomSyncSummary
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMembersLoadStatusType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberEventHandler
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface CreateLocalRoomTask : Task<CreateRoomParams, String>

internal class DefaultCreateLocalRoomTask @Inject constructor(
        @UserId private val userId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val roomMemberEventHandler: RoomMemberEventHandler,
        private val roomSummaryUpdater: RoomSummaryUpdater,
        @SessionDatabase private val realmConfiguration: RealmConfiguration,
        private val createRoomBodyBuilder: CreateRoomBodyBuilder,
        private val userService: UserService,
        private val clock: Clock,
) : CreateLocalRoomTask {

    override suspend fun execute(params: CreateRoomParams): String {
        val createRoomBody = createRoomBodyBuilder.build(params.withDefault())
        val roomId = RoomLocalEcho.createLocalEchoId()
        monarchy.awaitTransaction { realm ->
            createLocalRoomEntity(realm, roomId, createRoomBody)
            createLocalRoomSummaryEntity(realm, roomId, createRoomBody)
        }

        // Wait for room to be created in DB
        try {
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomSummaryEntity::class.java)
                        .equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
                        .equalTo(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
            }
        } catch (exception: TimeoutCancellationException) {
            throw CreateRoomFailure.CreatedWithTimeout(roomId)
        }

        return roomId
    }

    /**
     * Create a local room entity from the given room creation params.
     * This will also generate and store in database the chunk and the events related to the room params in order to retrieve and display the local room.
     */
    private suspend fun createLocalRoomEntity(realm: Realm, roomId: String, createRoomBody: CreateRoomBody) {
        RoomEntity.getOrCreate(realm, roomId).apply {
            membership = Membership.JOIN
            chunks.add(createLocalRoomChunk(realm, roomId, createRoomBody))
            membersLoadStatus = RoomMembersLoadStatusType.LOADED
        }
    }

    private fun createLocalRoomSummaryEntity(realm: Realm, roomId: String, createRoomBody: CreateRoomBody) {
        val otherUserId = createRoomBody.getDirectUserId()
        if (otherUserId != null) {
            RoomSummaryEntity.getOrCreate(realm, roomId).apply {
                isDirect = true
                directUserId = otherUserId
            }
        }
        roomSummaryUpdater.update(
                realm = realm,
                roomId = roomId,
                membership = Membership.JOIN,
                roomSummary = RoomSyncSummary(
                        heroes = createRoomBody.invitedUserIds.orEmpty().take(5),
                        joinedMembersCount = 1,
                        invitedMembersCount = createRoomBody.invitedUserIds?.size ?: 0
                ),
                updateMembers = !createRoomBody.invitedUserIds.isNullOrEmpty()
        )
    }

    /**
     * Create a single chunk containing the necessary events to display the local room.
     *
     * @param realm the current instance of realm
     * @param roomId the id of the local room
     * @param createRoomBody the room creation params
     *
     * @return a chunk entity
     */
    private suspend fun createLocalRoomChunk(realm: Realm, roomId: String, createRoomBody: CreateRoomBody): ChunkEntity {
        val chunkEntity = realm.createObject<ChunkEntity>().apply {
            isLastBackward = true
            isLastForward = true
        }

        val eventList = createLocalRoomEvents(createRoomBody)
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

        for (event in eventList) {
            if (event.eventId == null || event.senderId == null || event.type == null) {
                continue
            }

            val now = clock.epochMillis()
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, now).copyToRealmOrIgnore(realm, EventInsertType.INCREMENTAL_SYNC)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    roomMemberContentsByUser[event.stateKey] = event.getFixedRoomMemberContent()
                    roomMemberEventHandler.handle(realm, roomId, event, false)
                }
            }

            roomMemberContentsByUser.getOrPut(event.senderId) {
                // If we don't have any new state on this user, get it from db
                val rootStateEvent = CurrentStateEventEntity.getOrNull(realm, roomId, event.senderId, EventType.STATE_ROOM_MEMBER)?.root
                rootStateEvent?.asDomain()?.getFixedRoomMemberContent()
            }

            chunkEntity.addTimelineEvent(
                    roomId = roomId,
                    eventEntity = eventEntity,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = roomMemberContentsByUser
            )
        }

        return chunkEntity
    }

    /**
     * Build the list of the events related to the room creation params.
     *
     * @param createRoomBody the room creation params
     *
     * @return the list of events
     */
    private suspend fun createLocalRoomEvents(createRoomBody: CreateRoomBody): List<Event> {
        val myUser = userService.getUser(userId) ?: User(userId)
        val invitedUsers = createRoomBody.invitedUserIds.orEmpty()
                .mapNotNull { tryOrNull { userService.resolveUser(it) } }

        val createRoomEvent = createLocalEvent(
                type = EventType.STATE_ROOM_CREATE,
                content = RoomCreateContent(
                        creator = userId
                ).toContent()
        )
        val myRoomMemberEvent = createLocalEvent(
                type = EventType.STATE_ROOM_MEMBER,
                content = RoomMemberContent(
                        membership = Membership.JOIN,
                        displayName = myUser.displayName,
                        avatarUrl = myUser.avatarUrl
                ).toContent(),
                stateKey = userId
        )
        val roomMemberEvents = invitedUsers.map {
            createLocalEvent(
                    type = EventType.STATE_ROOM_MEMBER,
                    content = RoomMemberContent(
                            isDirect = createRoomBody.isDirect.orFalse(),
                            membership = Membership.INVITE,
                            displayName = it.displayName,
                            avatarUrl = it.avatarUrl
                    ).toContent(),
                    stateKey = it.userId
            )
        }

        return buildList {
            add(createRoomEvent)
            add(myRoomMemberEvent)
            addAll(createRoomBody.initialStates.orEmpty().map { createLocalEvent(it.type, it.content, it.stateKey) })
            addAll(roomMemberEvents)
        }
    }

    /**
     * Generate a local event from the given parameters.
     *
     * @param type the event type, see [EventType]
     * @param content the content of the Event
     * @param stateKey the stateKey, if any
     *
     * @return a fake event
     */
    private fun createLocalEvent(type: String?, content: Content?, stateKey: String? = ""): Event {
        return Event(
                type = type,
                senderId = userId,
                stateKey = stateKey,
                content = content,
                originServerTs = clock.epochMillis(),
                eventId = LocalEcho.createLocalEchoId()
        )
    }

    /**
     * Setup default values to the CreateRoomParams as the room is created locally (the default values will not be defined by the server).
     */
    private fun CreateRoomParams.withDefault() = this.apply {
        if (visibility == null) visibility = RoomDirectoryVisibility.PRIVATE
        if (historyVisibility == null) historyVisibility = RoomHistoryVisibility.SHARED
        if (guestAccess == null) guestAccess = GuestAccess.Forbidden
    }
}
