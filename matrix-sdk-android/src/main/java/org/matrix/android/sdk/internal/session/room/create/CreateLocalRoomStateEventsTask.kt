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

import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.toContent
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
import org.matrix.android.sdk.api.session.room.model.redactOrDefault
import org.matrix.android.sdk.api.session.room.model.stateDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.usersDefaultOrDefault
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.internal.session.room.create.CreateLocalRoomStateEventsTask.Params
import org.matrix.android.sdk.internal.session.room.membership.threepid.toThreePid
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal interface CreateLocalRoomStateEventsTask : Task<Params, List<Event>> {
    data class Params(
            val roomCreatorUserId: String,
            val createRoomBody: CreateRoomBody
    )
}

internal class DefaultCreateLocalRoomStateEventsTask @Inject constructor(
        private val userService: UserService,
        private val clock: Clock,
) : CreateLocalRoomStateEventsTask {

    private lateinit var createRoomBody: CreateRoomBody
    private lateinit var roomCreatorUserId: String

    override suspend fun execute(params: Params): List<Event> {
        createRoomBody = params.createRoomBody
        roomCreatorUserId = params.roomCreatorUserId

        return buildList {
            createRoomCreateEvent()
            createRoomMemberEvents(listOf(roomCreatorUserId))
            createRoomPowerLevelsEvent()
            createRoomAliasEvent()
            createRoomPresetEvents()
            createRoomInitialStateEvents()
            createRoomNameAndTopicStateEvents()
            createRoomMemberEvents(createRoomBody.invitedUserIds.orEmpty())
            createRoomThreePidEvents()
            createRoomDefaultEvents()
        }
    }

    /**
     * Generate the create state event related to this room.
     */
    private fun MutableList<Event>.createRoomCreateEvent() = apply {
        val roomCreateEvent = createLocalStateEvent(
                type = EventType.STATE_ROOM_CREATE,
                content = RoomCreateContent(
                        creator = roomCreatorUserId,
                        roomVersion = createRoomBody.roomVersion,
                        type = (createRoomBody.creationContent as? Map<*, *>)?.get(CreateRoomParams.CREATION_CONTENT_KEY_ROOM_TYPE) as? String

                ).toContent(),
        )
        add(roomCreateEvent)
    }

    /**
     * Generate the create state event related to the power levels using the given overridden values or the default values according to the specification.
     * Ref: https://spec.matrix.org/latest/client-server-api/#mroompower_levels
     */
    private fun MutableList<Event>.createRoomPowerLevelsEvent() = apply {
        val powerLevelsContent = createLocalStateEvent(
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
        )
        add(powerLevelsContent)
    }

    /**
     * Generate the local room member state events related to the given user ids, if any.
     */
    private suspend fun MutableList<Event>.createRoomMemberEvents(userIds: List<String>) = apply {
        val memberEvents = userIds
                .mapNotNull { tryOrNull { userService.resolveUser(it) } }
                .map { user ->
                    createLocalStateEvent(
                            type = EventType.STATE_ROOM_MEMBER,
                            content = RoomMemberContent(
                                    isDirect = createRoomBody.isDirect.takeUnless { user.userId == roomCreatorUserId }.orFalse(),
                                    membership = if (user.userId == roomCreatorUserId) Membership.JOIN else Membership.INVITE,
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
    private fun MutableList<Event>.createRoomThreePidEvents() = apply {
        val threePidEvents = createRoomBody.invite3pids.orEmpty().map { body ->
            val localThirdPartyInviteEvent = createLocalStateEvent(
                    type = EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE,
                    content = LocalRoomThirdPartyInviteContent(
                            isDirect = createRoomBody.isDirect.orFalse(),
                            membership = Membership.INVITE,
                            displayName = body.address,
                            thirdPartyInvite = body.toThreePid()
                    ).toContent(),
            )
            val thirdPartyInviteEvent = createLocalStateEvent(
                    type = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                    content = RoomThirdPartyInviteContent(body.address, null, null, null).toContent(),
            )
            listOf(localThirdPartyInviteEvent, thirdPartyInviteEvent)
        }.flatten()
        addAll(threePidEvents)
    }

    /**
     * Generate the local state event related to the given alias, if any.
     */
    fun MutableList<Event>.createRoomAliasEvent() = apply {
        if (createRoomBody.roomAliasName != null) {
            val canonicalAliasContent = createLocalStateEvent(
                    type = EventType.STATE_ROOM_CANONICAL_ALIAS,
                    content = RoomCanonicalAliasContent(
                            canonicalAlias = "${createRoomBody.roomAliasName}:${roomCreatorUserId.getServerName()}"
                    ).toContent(),
            )
            add(canonicalAliasContent)
        }
    }

    /**
     * Generate the local state events related to the given [CreateRoomPreset].
     * Ref: https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3createroom
     */
    private fun MutableList<Event>.createRoomPresetEvents() = apply {
        val preset = createRoomBody.preset ?: return@apply

        var joinRules: RoomJoinRules? = null
        var historyVisibility: RoomHistoryVisibility? = null
        var guestAccess: GuestAccess? = null
        when (preset) {
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

        add(createLocalStateEvent(EventType.STATE_ROOM_JOIN_RULES, RoomJoinRulesContent(joinRules.value).toContent()))
        add(createLocalStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY, RoomHistoryVisibilityContent(historyVisibility.value).toContent()))
        add(createLocalStateEvent(EventType.STATE_ROOM_GUEST_ACCESS, RoomGuestAccessContent(guestAccess.value).toContent()))
    }

    /**
     * Generate the local state events related to the given initial states, if any.
     * The given initial state events override the potential existing ones of the same type.
     */
    private fun MutableList<Event>.createRoomInitialStateEvents() = apply {
        val initialStates = createRoomBody.initialStates ?: return@apply

        val initialStateEvents = initialStates.map { createLocalStateEvent(it.type, it.content, it.stateKey) }
        // Erase existing events of the same type
        removeAll { event -> event.type in initialStateEvents.map { it.type } }
        // Add the initial state events to the list
        addAll(initialStateEvents)
    }

    /**
     * Generate the local events related to the given room name and topic, if any.
     */
    private fun MutableList<Event>.createRoomNameAndTopicStateEvents() = apply {
        if (createRoomBody.name != null) {
            add(createLocalStateEvent(EventType.STATE_ROOM_NAME, RoomNameContent(createRoomBody.name).toContent()))
        }
        if (createRoomBody.topic != null) {
            add(createLocalStateEvent(EventType.STATE_ROOM_TOPIC, RoomTopicContent(createRoomBody.topic).toContent()))
        }
    }

    /**
     * Generate the local events which have not been set and are in that case provided by the server with default values.
     * Default events:
     * - m.room.history_visibility (https://spec.matrix.org/latest/client-server-api/#server-behaviour-5)
     * - m.room.guest_access (https://spec.matrix.org/latest/client-server-api/#mroomguest_access)
     */
    private fun MutableList<Event>.createRoomDefaultEvents() = apply {
        // HistoryVisibility
        if (none { it.type == EventType.STATE_ROOM_HISTORY_VISIBILITY }) {
            add(
                    createLocalStateEvent(
                            type = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                            content = RoomHistoryVisibilityContent(RoomHistoryVisibility.SHARED.value).toContent(),
                    )
            )
        }
        // GuestAccess
        if (none { it.type == EventType.STATE_ROOM_GUEST_ACCESS }) {
            add(
                    createLocalStateEvent(
                            type = EventType.STATE_ROOM_GUEST_ACCESS,
                            content = RoomGuestAccessContent(GuestAccess.Forbidden.value).toContent(),
                    )
            )
        }
    }

    /**
     * Generate a local state event from the given parameters.
     *
     * @param type the event type, see [EventType]
     * @param content the content of the event
     * @param stateKey the stateKey, if any
     *
     * @return a local state event
     */
    private fun createLocalStateEvent(type: String?, content: Content?, stateKey: String? = ""): Event {
        return Event(
                type = type,
                senderId = roomCreatorUserId,
                stateKey = stateKey,
                content = content,
                originServerTs = clock.epochMillis(),
                eventId = LocalEcho.createLocalEchoId()
        )
    }
}
