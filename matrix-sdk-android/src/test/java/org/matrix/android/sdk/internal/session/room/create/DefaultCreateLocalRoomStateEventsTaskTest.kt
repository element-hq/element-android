/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.create

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.identity.ThreePid
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
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.session.profile.ThirdPartyIdentifier.Companion.MEDIUM_EMAIL
import org.matrix.android.sdk.internal.session.profile.ThirdPartyIdentifier.Companion.MEDIUM_MSISDN
import org.matrix.android.sdk.internal.session.room.membership.threepid.ThreePidInviteBody
import org.matrix.android.sdk.internal.session.room.membership.threepid.toThreePid
import org.matrix.android.sdk.internal.util.time.DefaultClock

private const val MY_USER_ID = "my-user-id"
private const val MY_USER_DISPLAY_NAME = "my-user-display-name"
private const val MY_USER_AVATAR = "my-user-avatar"

@ExperimentalCoroutinesApi
internal class DefaultCreateLocalRoomStateEventsTaskTest {

    private val clock = DefaultClock()
    private val userService = mockk<UserService>()

    private val defaultCreateLocalRoomStateEventsTask = DefaultCreateLocalRoomStateEventsTask(
            myUserId = MY_USER_ID,
            userService = userService,
            clock = clock
    )

    lateinit var createRoomBody: CreateRoomBody

    @Before
    fun setup() {
        createRoomBody = mockk {
            every { roomVersion } returns null
            every { creationContent } returns null
            every { roomAliasName } returns null
            every { topic } returns null
            every { name } returns null
            every { powerLevelContentOverride } returns null
            every { initialStates } returns null
            every { invite3pids } returns null
            every { preset } returns null
            every { isDirect } returns null
            every { invitedUserIds } returns null
        }
        coEvery { userService.resolveUser(any()) } answers { User(firstArg()) }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct room create state event`() = runTest {
        // Given
        val aRoomVersion = "a_room_version"

        every { createRoomBody.roomVersion } returns aRoomVersion

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        val roomCreateEvent = result.find { it.type == EventType.STATE_ROOM_CREATE }
        val roomCreateContent = roomCreateEvent?.content.toModel<RoomCreateContent>()

        roomCreateContent?.creator shouldBeEqualTo MY_USER_ID
        roomCreateContent?.roomVersion shouldBeEqualTo aRoomVersion
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct name and topic state events`() = runTest {
        // Given
        val aRoomName = "a_room_name"
        val aRoomTopic = "a_room_topic"

        every { createRoomBody.name } returns aRoomName
        every { createRoomBody.topic } returns aRoomTopic

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        val roomNameEvent = result.find { it.type == EventType.STATE_ROOM_NAME }
        val roomTopicEvent = result.find { it.type == EventType.STATE_ROOM_TOPIC }

        roomNameEvent?.content.toModel<RoomNameContent>()?.name shouldBeEqualTo aRoomName
        roomTopicEvent?.content.toModel<RoomTopicContent>()?.topic shouldBeEqualTo aRoomTopic
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct room member events`() = runTest {
        // Given
        data class RoomMember(val user: User, val membership: Membership)

        val aRoomMemberList: List<RoomMember> = listOf(
                RoomMember(User(MY_USER_ID, MY_USER_DISPLAY_NAME, MY_USER_AVATAR), Membership.JOIN),
                RoomMember(User("userA_id", "userA_display_name", "userA_avatar"), Membership.INVITE),
                RoomMember(User("userB_id", "userB_display_name", "userB_avatar"), Membership.INVITE)
        )

        every { createRoomBody.invitedUserIds } returns aRoomMemberList.filter { it.membership == Membership.INVITE }.map { it.user.userId }
        coEvery { userService.resolveUser(any()) } answers {
            aRoomMemberList.map { it.user }.find { it.userId == firstArg() } ?: User(firstArg())
        }

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        val roomMemberEvents = result.filter { it.type == EventType.STATE_ROOM_MEMBER }

        roomMemberEvents.map { it.stateKey } shouldBeEqualTo aRoomMemberList.map { it.user.userId }
        roomMemberEvents.forEach { event ->
            val roomMemberContent = event.content.toModel<RoomMemberContent>()
            val roomMember = aRoomMemberList.find { it.user.userId == event.stateKey }

            roomMember.shouldNotBeNull()
            roomMemberContent?.avatarUrl shouldBeEqualTo roomMember.user.avatarUrl
            roomMemberContent?.displayName shouldBeEqualTo roomMember.user.displayName
            roomMemberContent?.membership shouldBeEqualTo roomMember.membership
        }
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct power levels event`() = runTest {
        // Given
        val aPowerLevelsContent = PowerLevelsContent(
                ban = 1,
                kick = 2,
                invite = 3,
                redact = 4,
                eventsDefault = 5,
                events = null,
                usersDefault = 6,
                users = null,
                stateDefault = 7,
                notifications = null
        )

        every { createRoomBody.powerLevelContentOverride } returns aPowerLevelsContent

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        val roomPowerLevelsEvent = result.find { it.type == EventType.STATE_ROOM_POWER_LEVELS }
        roomPowerLevelsEvent?.content.toModel<PowerLevelsContent>() shouldBeEqualTo aPowerLevelsContent
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct canonical alias event`() = runTest {
        // Given
        val aRoomAlias = "a_room_alias"
        val expectedCanonicalAlias = "$aRoomAlias:${MY_USER_ID.getServerName()}"

        every { createRoomBody.roomAliasName } returns aRoomAlias

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        val roomPowerLevelsEvent = result.find { it.type == EventType.STATE_ROOM_CANONICAL_ALIAS }
        roomPowerLevelsEvent?.content.toModel<RoomCanonicalAliasContent>()?.canonicalAlias shouldBeEqualTo expectedCanonicalAlias
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct preset related events`() = runTest {
        data class ExpectedResult(val joinRules: RoomJoinRules, val historyVisibility: RoomHistoryVisibility, val guestAccess: GuestAccess)
        data class Case(val preset: CreateRoomPreset, val expectedResult: ExpectedResult)

        CreateRoomPreset.values().forEach { aRoomPreset ->
            // Given
            val case = when (aRoomPreset) {
                CreateRoomPreset.PRESET_PRIVATE_CHAT -> Case(
                        CreateRoomPreset.PRESET_PRIVATE_CHAT,
                        ExpectedResult(RoomJoinRules.INVITE, RoomHistoryVisibility.SHARED, GuestAccess.CanJoin)
                )
                CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT -> Case(
                        CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT,
                        ExpectedResult(RoomJoinRules.INVITE, RoomHistoryVisibility.SHARED, GuestAccess.CanJoin)
                )
                CreateRoomPreset.PRESET_PUBLIC_CHAT -> Case(
                        CreateRoomPreset.PRESET_PUBLIC_CHAT,
                        ExpectedResult(RoomJoinRules.PUBLIC, RoomHistoryVisibility.SHARED, GuestAccess.Forbidden)
                )
            }
            every { createRoomBody.preset } returns case.preset

            // When
            val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
            val result = defaultCreateLocalRoomStateEventsTask.execute(params)

            // Then
            result.find { it.type == EventType.STATE_ROOM_JOIN_RULES }
                    ?.content.toModel<RoomJoinRulesContent>()
                    ?.joinRules shouldBeEqualTo case.expectedResult.joinRules
            result.find { it.type == EventType.STATE_ROOM_HISTORY_VISIBILITY }
                    ?.content.toModel<RoomHistoryVisibilityContent>()
                    ?.historyVisibility shouldBeEqualTo case.expectedResult.historyVisibility
            result.find { it.type == EventType.STATE_ROOM_GUEST_ACCESS }
                    ?.content.toModel<RoomGuestAccessContent>()
                    ?.guestAccess shouldBeEqualTo case.expectedResult.guestAccess
        }
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the initial state events`() = runTest {
        // Given
        val aListOfInitialStateEvents = listOf(
                Event(
                        type = EventType.STATE_ROOM_ENCRYPTION,
                        stateKey = "",
                        content = EncryptionEventContent(MXCRYPTO_ALGORITHM_MEGOLM).toContent()
                ),
                Event(
                        type = "a_custom_type",
                        content = mapOf("a_custom_map_to_integer" to 42),
                        stateKey = "a_state_key"
                ),
                Event(
                        type = "another_custom_type",
                        content = mapOf("a_custom_map_to_boolean" to false),
                        stateKey = "another_state_key"
                )
        )

        every { createRoomBody.initialStates } returns aListOfInitialStateEvents

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        aListOfInitialStateEvents.forEach { expected ->
            val found = result.find { it.type == expected.type }
            found.shouldNotBeNull()
            found.content shouldBeEqualTo expected.content
            found.stateKey shouldBeEqualTo expected.stateKey
        }
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events contains the correct third party invite events`() = runTest {
        // Given
        val aListOfThreePids = listOf(
                ThreePid.Email("bob@matrix.org"),
                ThreePid.Msisdn("+11111111111"),
                ThreePid.Email("alice@matrix.org"),
                ThreePid.Msisdn("+22222222222"),
        )
        val aListOf3pids = aListOfThreePids.mapIndexed { index, threePid ->
            ThreePidInviteBody(
                    idServer = "an_id_server_$index",
                    idAccessToken = "an_id_access_token_$index",
                    medium = when (threePid) {
                        is ThreePid.Email -> MEDIUM_EMAIL
                        is ThreePid.Msisdn -> MEDIUM_MSISDN
                    },
                    address = threePid.value
            )
        }
        every { createRoomBody.invite3pids } returns aListOf3pids

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        val thirdPartyInviteEvents = result.filter { it.type == EventType.STATE_ROOM_THIRD_PARTY_INVITE }
        val thirdPartyInviteContents = thirdPartyInviteEvents.map { it.content.toModel<RoomThirdPartyInviteContent>() }
        val localThirdPartyInviteEvents = result.filter { it.type == EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE }
        val localThirdPartyInviteContents = localThirdPartyInviteEvents.map { it.content.toModel<LocalRoomThirdPartyInviteContent>() }

        thirdPartyInviteEvents.size shouldBeEqualTo aListOf3pids.size
        localThirdPartyInviteEvents.size shouldBeEqualTo aListOf3pids.size

        aListOf3pids.forEach { expected ->
            thirdPartyInviteContents.find { it?.displayName == expected.address }.shouldNotBeNull()

            val localThirdPartyInviteContent = localThirdPartyInviteContents.find { it?.thirdPartyInvite == expected.toThreePid() }
            localThirdPartyInviteContent.shouldNotBeNull()
            localThirdPartyInviteContent.membership shouldBeEqualTo Membership.INVITE
            localThirdPartyInviteContent.isDirect shouldBeEqualTo createRoomBody.isDirect.orFalse()
            localThirdPartyInviteContent.displayName shouldBeEqualTo expected.address
        }
    }

    @Test
    fun `given a CreateRoomBody with default values when execute then the resulting list of events is correct`() = runTest {
        // Given
        // map of expected event types to occurrences
        val expectedEventTypes = mapOf(
                EventType.STATE_ROOM_CREATE to 1,
                EventType.STATE_ROOM_POWER_LEVELS to 1,
                EventType.STATE_ROOM_MEMBER to 1,
                EventType.STATE_ROOM_GUEST_ACCESS to 1,
                EventType.STATE_ROOM_HISTORY_VISIBILITY to 1,
        )
        coEvery { userService.resolveUser(any()) } answers {
            if (firstArg<String>() == MY_USER_ID) User(MY_USER_ID, MY_USER_DISPLAY_NAME, MY_USER_AVATAR) else User(firstArg())
        }

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        result.size shouldBeEqualTo expectedEventTypes.values.sum()
        result.map { it.type }.toSet() shouldBeEqualTo expectedEventTypes.keys

        // Room create
        result.find { it.type == EventType.STATE_ROOM_CREATE }.shouldNotBeNull()
        // Room member
        result.singleOrNull { it.type == EventType.STATE_ROOM_MEMBER }?.stateKey shouldBeEqualTo MY_USER_ID
        // Power levels
        val powerLevelsContent = result.find { it.type == EventType.STATE_ROOM_POWER_LEVELS }?.content.toModel<PowerLevelsContent>()
        powerLevelsContent.shouldNotBeNull()
        powerLevelsContent.ban shouldBeEqualTo Role.Moderator.value
        powerLevelsContent.kick shouldBeEqualTo Role.Moderator.value
        powerLevelsContent.invite shouldBeEqualTo Role.Moderator.value
        powerLevelsContent.redact shouldBeEqualTo Role.Moderator.value
        powerLevelsContent.eventsDefault shouldBeEqualTo Role.Default.value
        powerLevelsContent.usersDefault shouldBeEqualTo Role.Default.value
        powerLevelsContent.stateDefault shouldBeEqualTo Role.Moderator.value
        // Guest access
        result.find { it.type == EventType.STATE_ROOM_GUEST_ACCESS }
                ?.content.toModel<RoomGuestAccessContent>()?.guestAccess shouldBeEqualTo GuestAccess.Forbidden
        // History visibility
        result.find { it.type == EventType.STATE_ROOM_HISTORY_VISIBILITY }
                ?.content.toModel<RoomHistoryVisibilityContent>()?.historyVisibility shouldBeEqualTo RoomHistoryVisibility.SHARED
    }

    @Test
    fun `given a CreateRoomBody when execute then the resulting list of events is correctly ordered with the right values`() = runTest {
        // Given
        val expectedIsDirect = true
        val expectedHistoryVisibility = RoomHistoryVisibility.WORLD_READABLE

        every { createRoomBody.roomVersion } returns "a_room_version"
        every { createRoomBody.roomAliasName } returns "a_room_alias_name"
        every { createRoomBody.name } returns "a_name"
        every { createRoomBody.topic } returns "a_topic"
        every { createRoomBody.powerLevelContentOverride } returns PowerLevelsContent(
                ban = 1,
                kick = 2,
                invite = 3,
                redact = 4,
                eventsDefault = 5,
                events = null,
                usersDefault = 6,
                users = null,
                stateDefault = 7,
                notifications = null
        )
        every { createRoomBody.invite3pids } returns listOf(
                ThreePidInviteBody(
                        idServer = "an_id_server",
                        idAccessToken = "an_id_access_token",
                        medium = MEDIUM_EMAIL,
                        address = "an_email@example.org"
                )
        )
        every { createRoomBody.preset } returns CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
        every { createRoomBody.initialStates } returns listOf(
                Event(type = "a_custom_type", stateKey = ""),
                // override the value from the preset
                Event(
                        type = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                        stateKey = "",
                        content = RoomHistoryVisibilityContent(expectedHistoryVisibility.value).toContent()
                )
        )
        every { createRoomBody.isDirect } returns expectedIsDirect
        every { createRoomBody.invitedUserIds } returns listOf("a_user_id")

        val orderedExpectedEventType = listOf(
                EventType.STATE_ROOM_CREATE,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_ROOM_POWER_LEVELS,
                EventType.STATE_ROOM_CANONICAL_ALIAS,
                EventType.STATE_ROOM_JOIN_RULES,
                EventType.STATE_ROOM_GUEST_ACCESS,
                "a_custom_type",
                EventType.STATE_ROOM_HISTORY_VISIBILITY,
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_TOPIC,
                EventType.STATE_ROOM_MEMBER,
                EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
        )

        // When
        val params = CreateLocalRoomStateEventsTask.Params(createRoomBody)
        val result = defaultCreateLocalRoomStateEventsTask.execute(params)

        // Then
        result.map { it.type } shouldBeEqualTo orderedExpectedEventType
        result.find { it.type == EventType.STATE_ROOM_HISTORY_VISIBILITY }
                ?.content.toModel<RoomHistoryVisibilityContent>()?.historyVisibility shouldBeEqualTo expectedHistoryVisibility
        result.lastOrNull { it.type == EventType.STATE_ROOM_MEMBER }
                ?.content.toModel<RoomMemberContent>()?.isDirect shouldBeEqualTo expectedIsDirect
        result.lastOrNull { it.type == EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE }
                ?.content.toModel<LocalRoomThirdPartyInviteContent>()?.isDirect shouldBeEqualTo expectedIsDirect
    }
}
