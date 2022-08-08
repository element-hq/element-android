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
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
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
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.model.RoomGuestAccessContent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.model.RoomThirdPartyInviteContent
import org.matrix.android.sdk.api.session.room.model.RoomTopicContent
import org.matrix.android.sdk.api.session.room.model.banOrDefault
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.model.eventsDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.inviteOrDefault
import org.matrix.android.sdk.api.session.room.model.kickOrDefault
import org.matrix.android.sdk.api.session.room.model.localecho.LocalRoomThirdPartyInviteContent
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.room.model.redactOrDefault
import org.matrix.android.sdk.api.session.room.model.stateDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.usersDefaultOrDefault
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.sync.model.RoomSyncSummary
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
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
import org.matrix.android.sdk.internal.session.room.membership.threepid.toThreePid
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface CreateLocalRoomTask : Task<CreateRoomParams, String>

internal class DefaultCreateLocalRoomTask @Inject constructor(
        @UserId private val myUserId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val roomMemberEventHandler: RoomMemberEventHandler,
        private val roomSummaryUpdater: RoomSummaryUpdater,
        @SessionDatabase private val realmConfiguration: RealmConfiguration,
        private val createRoomBodyBuilder: CreateRoomBodyBuilder,
        private val userService: UserService,
        private val cryptoService: DefaultCryptoService,
        private val clock: Clock,
) : CreateLocalRoomTask {

    override suspend fun execute(params: CreateRoomParams): String {
        val createRoomBody = createRoomBodyBuilder.build(params)
        val roomId = RoomLocalEcho.createLocalEchoId()
        monarchy.awaitTransaction { realm ->
            createLocalRoomEntity(realm, roomId, createRoomBody)
            createLocalRoomSummaryEntity(realm, roomId, params, createRoomBody)
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

    private fun createLocalRoomSummaryEntity(realm: Realm, roomId: String, createRoomParams: CreateRoomParams, createRoomBody: CreateRoomBody) {
        // Create the room summary entity
        val roomSummaryEntity = realm.createObject<RoomSummaryEntity>(roomId).apply {
            val otherUserId = createRoomBody.getDirectUserId()
            if (otherUserId != null) {
                isDirect = true
                directUserId = otherUserId
            }
        }

        // Update the createRoomParams from the potential feature preset before saving
        createRoomParams.featurePreset?.let { featurePreset ->
            featurePreset.updateRoomParams(createRoomParams)
            createRoomParams.initialStates.addAll(featurePreset.setupInitialStates().orEmpty())
        }

        // Create a LocalRoomSummaryEntity decorated by the related RoomSummaryEntity and the updated CreateRoomParams
        realm.createObject<LocalRoomSummaryEntity>(roomId).also {
            it.roomSummaryEntity = roomSummaryEntity
            it.createRoomParams = createRoomParams
        }

        // Update the RoomSummaryEntity by simulating a fake sync response
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

                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
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
     * Build the list of state events related to the room creation body.
     * The events list is ordered following the specification: https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3createroom
     *
     * @param createRoomBody the room creation body
     *
     * @return the list of events
     */
    private suspend fun createLocalRoomEvents(createRoomBody: CreateRoomBody): List<Event> {
        return buildList {
            createRoomCreateEvent(createRoomBody)
            createRoomMemberEvents(listOf(myUserId))
            createRoomPowerLevelsEvent(createRoomBody)
            createRoomAliasEvent(createRoomBody)
            createRoomPresetEvents(createRoomBody)
            createRoomInitialStateEvents(createRoomBody)
            createRoomNameAndTopicStateEvents(createRoomBody)
            createRoomMemberEvents(createRoomBody.invitedUserIds.orEmpty())
            createRoomThreePidEvents(createRoomBody)
            createRoomDefaultEvents()
        }
    }

    /**
     * Generate the create state event related to this room.
     */
    private fun MutableList<Event>.createRoomCreateEvent(createRoomBody: CreateRoomBody) = apply {
        val roomCreateEvent = createLocalEvent(
                type = EventType.STATE_ROOM_CREATE,
                content = RoomCreateContent(
                        creator = myUserId,
                        roomVersion = createRoomBody.roomVersion,
                        type = (createRoomBody.creationContent as? Map<*, *>)?.get(CreateRoomParams.CREATION_CONTENT_KEY_ROOM_TYPE) as? String

                ).toContent(),
                stateKey = ""
        )
        add(roomCreateEvent)
    }

    /**
     * Generate the create state event related to the power levels using the given overridden values or the default values according to the specification.
     * Ref: https://spec.matrix.org/latest/client-server-api/#mroompower_levels
     */
    private fun MutableList<Event>.createRoomPowerLevelsEvent(createRoomBody: CreateRoomBody) = apply {
        val powerLevelsContent = createLocalEvent(
                type = EventType.STATE_ROOM_POWER_LEVELS,
                content = (createRoomBody.powerLevelContentOverride ?: PowerLevelsContent()).let {
                    it.copy(
                            ban = it.banOrDefault(),
                            eventsDefault = it.eventsDefaultOrDefault(),
                            invite = it.inviteOrDefault(),
                            kick = it.kickOrDefault(),
                            redact = it.redactOrDefault(),
                            stateDefault = it.stateDefaultOrDefault(),
                            usersDefault = it.usersDefaultOrDefault(),
                    )
                }.toContent(),
                stateKey = ""
        )
        add(powerLevelsContent)
    }

    /**
     * Generate the local room member state events related to the given user ids, if any.
     */
    private suspend fun MutableList<Event>.createRoomMemberEvents(userIds: List<String>, isDirect: Boolean? = null) = apply {
        val memberEvents = userIds
                .mapNotNull { tryOrNull { userService.resolveUser(it) } }
                .map { user ->
                    createLocalEvent(
                            type = EventType.STATE_ROOM_MEMBER,
                            content = RoomMemberContent(
                                    isDirect = isDirect.orFalse(),
                                    membership = if (user.userId == myUserId) Membership.JOIN else Membership.INVITE,
                                    displayName = user.displayName,
                                    avatarUrl = user.avatarUrl
                            ).toContent(),
                            stateKey = user.userId
                    )
                }
        addAll(memberEvents)
    }

    /**
     * Generate the local state events related to the given third party invites, if any.
     */
    private fun MutableList<Event>.createRoomThreePidEvents(createRoomBody: CreateRoomBody) = apply {
        val threePidEvents = createRoomBody.invite3pids.orEmpty().map { body ->
            val localThirdPartyInviteEvent = createLocalEvent(
                    type = EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE,
                    content = LocalRoomThirdPartyInviteContent(
                            isDirect = createRoomBody.isDirect.orFalse(),
                            membership = Membership.INVITE,
                            displayName = body.address,
                            thirdPartyInvite = body.toThreePid()
                    ).toContent(),
                    stateKey = ""
            )
            val thirdPartyInviteEvent = createLocalEvent(
                    type = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                    content = RoomThirdPartyInviteContent(body.address, null, null, null).toContent(),
                    stateKey = ""
            )
            listOf(localThirdPartyInviteEvent, thirdPartyInviteEvent)
        }.flatten()
        addAll(threePidEvents)
    }

    /**
     * Generate the local state event related to the given alias, if any.
     */
    fun MutableList<Event>.createRoomAliasEvent(createRoomBody: CreateRoomBody) = apply {
        if (createRoomBody.roomAliasName != null) {
            val canonicalAliasContent = createLocalEvent(
                    type = EventType.STATE_ROOM_CANONICAL_ALIAS,
                    content = RoomCanonicalAliasContent(
                            canonicalAlias = "${createRoomBody.roomAliasName}:${myUserId.getServerName()}"
                    ).toContent(),
                    stateKey = ""
            )
            add(canonicalAliasContent)
        }
    }

    /**
     * Generate the local state events related to the given [CreateRoomPreset].
     * Ref: https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3createroom
     */
    private fun MutableList<Event>.createRoomPresetEvents(createRoomBody: CreateRoomBody) = apply {
        createRoomBody.preset ?: return@apply

        var joinRules: RoomJoinRules? = null
        var historyVisibility: RoomHistoryVisibility? = null
        var guestAccess: GuestAccess? = null
        when (createRoomBody.preset) {
            CreateRoomPreset.PRESET_PRIVATE_CHAT,
            CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT -> {
                joinRules = RoomJoinRules.INVITE
                historyVisibility = RoomHistoryVisibility.SHARED
                guestAccess = GuestAccess.CanJoin
            }
            CreateRoomPreset.PRESET_PUBLIC_CHAT -> {
                joinRules = RoomJoinRules.PUBLIC
                historyVisibility = RoomHistoryVisibility.SHARED
                guestAccess = GuestAccess.Forbidden
            }
        }

        add(createLocalEvent(EventType.STATE_ROOM_JOIN_RULES, RoomJoinRulesContent(joinRules.value).toContent(), ""))
        add(createLocalEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY, RoomHistoryVisibilityContent(historyVisibility.value).toContent(), ""))
        add(createLocalEvent(EventType.STATE_ROOM_GUEST_ACCESS, RoomGuestAccessContent(guestAccess.value).toContent(), ""))
    }

    /**
     * Generate the local state events related to the given initial states, if any.
     * The given initial state events override the potential existing ones of the same type.
     */
    private fun MutableList<Event>.createRoomInitialStateEvents(createRoomBody: CreateRoomBody) = apply {
        createRoomBody.initialStates ?: return@apply

        val initialStateEvents = createRoomBody.initialStates.map { createLocalEvent(it.type, it.content, it.stateKey) }
        // Erase existing events of the same type
        removeAll { event -> event.type in initialStateEvents.map { it.type } }
        // Add the initial state events to the list
        addAll(initialStateEvents)
    }

    /**
     * Generate the local events related to the given room name and topic, if any.
     */
    private fun MutableList<Event>.createRoomNameAndTopicStateEvents(createRoomBody: CreateRoomBody) = apply {
        if (createRoomBody.name != null) {
            add(createLocalEvent(EventType.STATE_ROOM_NAME, RoomNameContent(createRoomBody.name).toContent(), ""))
        }
        if (createRoomBody.topic != null) {
            add(createLocalEvent(EventType.STATE_ROOM_TOPIC, RoomTopicContent(createRoomBody.topic).toContent(), ""))
        }
    }

    /**
     * Generate the local events which have not been set and are in that case provided by the server with default values:
     * - m.room.history_visibility (https://spec.matrix.org/latest/client-server-api/#server-behaviour-5)
     * - m.room.guest_access (https://spec.matrix.org/latest/client-server-api/#mroomguest_access)
     */
    private fun MutableList<Event>.createRoomDefaultEvents() = apply {
        // HistoryVisibility
        if (none { it.type == EventType.STATE_ROOM_HISTORY_VISIBILITY }) {
            add(
                    createLocalEvent(
                            type = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                            content = RoomHistoryVisibilityContent(RoomHistoryVisibility.SHARED.value).toContent(),
                            stateKey = ""
                    )
            )
        }
        // GuestAccess
        if (none { it.type == EventType.STATE_ROOM_GUEST_ACCESS }) {
            add(
                    createLocalEvent(
                            type = EventType.STATE_ROOM_GUEST_ACCESS,
                            content = RoomGuestAccessContent(GuestAccess.Forbidden.value).toContent(),
                            stateKey = ""
                    )
            )
        }
    }

    /**
     * Generate a local event from the given parameters.
     *
     * @param type the event type, see [EventType]
     * @param content the content of the Event
     * @param stateKey the stateKey, if any
     *
     * @return a local event
     */
    private fun createLocalEvent(type: String?, content: Content?, stateKey: String?): Event {
        return Event(
                type = type,
                senderId = myUserId,
                stateKey = stateKey,
                content = content,
                originServerTs = clock.epochMillis(),
                eventId = LocalEcho.createLocalEchoId()
        )
    }
}
