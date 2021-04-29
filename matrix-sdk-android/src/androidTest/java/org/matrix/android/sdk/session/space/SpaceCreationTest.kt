/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.session.space

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomGuestAccessContent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.space.JoinSpaceResult
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SpaceCreationTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    @Test
    fun createSimplePublicSpace() {
        val session = commonTestHelper.createAccount("Hubble", SessionTestParams(true))
        val roomName = "My Space"
        val topic = "A public space for test"
        val spaceId: String
        runBlocking {
            spaceId = session.spaceService().createSpace(roomName, topic, null, true)
            // wait a bit to let the summary update it self :/
            delay(400)
        }

        val syncedSpace = session.spaceService().getSpace(spaceId)
        assertEquals(roomName, syncedSpace?.asRoom()?.roomSummary()?.name, "Room name should be set")
        assertEquals(topic, syncedSpace?.asRoom()?.roomSummary()?.topic, "Room topic should be set")
        // assertEquals(topic, syncedSpace.asRoom().roomSummary()?., "Room topic should be set")

        assertNotNull(syncedSpace, "Space should be found by Id")
        val creationEvent = syncedSpace.asRoom().getStateEvent(EventType.STATE_ROOM_CREATE)
        val createContent = creationEvent?.content.toModel<RoomCreateContent>()
        assertEquals(RoomType.SPACE, createContent?.type, "Room type should be space")

        var powerLevelsContent: PowerLevelsContent? = null
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val toModel = syncedSpace.asRoom().getStateEvent(EventType.STATE_ROOM_POWER_LEVELS)?.content.toModel<PowerLevelsContent>()
                powerLevelsContent = toModel
                toModel != null
            }
        }
        assertEquals(100, powerLevelsContent?.eventsDefault, "Space-rooms should be created with a power level for events_default of 100")

        val guestAccess = syncedSpace.asRoom().getStateEvent(EventType.STATE_ROOM_GUEST_ACCESS)?.content
                ?.toModel<RoomGuestAccessContent>()?.guestAccess

        assertEquals(GuestAccess.CanJoin, guestAccess, "Public space room should be peekable by guest")

        val historyVisibility = syncedSpace.asRoom().getStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY)?.content
                ?.toModel<RoomHistoryVisibilityContent>()?.historyVisibility

        assertEquals(RoomHistoryVisibility.WORLD_READABLE, historyVisibility, "Public space room should be world readable")

        commonTestHelper.signOutAndClose(session)
    }

    @Test
    fun testJoinSimplePublicSpace() {
        val aliceSession = commonTestHelper.createAccount("alice", SessionTestParams(true))
        val bobSession = commonTestHelper.createAccount("bob", SessionTestParams(true))

        val roomName = "My Space"
        val topic = "A public space for test"
        val spaceId: String
        runBlocking {
            spaceId = aliceSession.spaceService().createSpace(roomName, topic, null, true)
            // wait a bit to let the summary update it self :/
            delay(400)
        }

        // Try to join from bob, it's a public space no need to invite

        val joinResult: JoinSpaceResult
        runBlocking {
            joinResult = bobSession.spaceService().joinSpace(spaceId)
        }

        assertEquals(JoinSpaceResult.Success, joinResult)

        val spaceBobPov = bobSession.spaceService().getSpace(spaceId)
        assertEquals(roomName, spaceBobPov?.asRoom()?.roomSummary()?.name, "Room name should be set")
        assertEquals(topic, spaceBobPov?.asRoom()?.roomSummary()?.topic, "Room topic should be set")

        commonTestHelper.signOutAndClose(aliceSession)
        commonTestHelper.signOutAndClose(bobSession)
    }

    @Test
    fun testSimplePublicSpaceWithChildren() {
        val aliceSession = commonTestHelper.createAccount("alice", SessionTestParams(true))
        val bobSession = commonTestHelper.createAccount("bob", SessionTestParams(true))

        val roomName = "My Space"
        val topic = "A public space for test"

        val spaceId: String = runBlocking { aliceSession.spaceService().createSpace(roomName, topic, null, true) }
        val syncedSpace = aliceSession.spaceService().getSpace(spaceId)

        // create a room
        val firstChild: String = runBlocking {
            aliceSession.createRoom(CreateRoomParams().apply {
                this.name = "FirstRoom"
                this.topic = "Description of first room"
                this.preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
            })
        }

        runBlocking {
            syncedSpace?.addChildren(firstChild, listOf(aliceSession.sessionParams.homeServerHost ?: ""), "a", true)
        }

        val secondChild: String = runBlocking {
            aliceSession.createRoom(CreateRoomParams().apply {
                this.name = "SecondRoom"
                this.topic = "Description of second room"
                this.preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
            })
        }

        runBlocking {
            syncedSpace?.addChildren(secondChild, listOf(aliceSession.sessionParams.homeServerHost ?: ""), "b", false)
        }

        // Try to join from bob, it's a public space no need to invite

        val joinResult = runBlocking {
            bobSession.spaceService().joinSpace(spaceId)
        }

        assertEquals(JoinSpaceResult.Success, joinResult)

        val spaceBobPov = bobSession.spaceService().getSpace(spaceId)
        assertEquals(roomName, spaceBobPov?.asRoom()?.roomSummary()?.name, "Room name should be set")
        assertEquals(topic, spaceBobPov?.asRoom()?.roomSummary()?.topic, "Room topic should be set")

        // check if bob has joined automatically the first room

        val bobMembershipFirstRoom = bobSession.getRoom(firstChild)?.roomSummary()?.membership
        assertEquals(Membership.JOIN, bobMembershipFirstRoom, "Bob should have joined this room")
        RoomSummaryQueryParams.Builder()

        val spaceSummaryBobPov = bobSession.spaceService().getSpaceSummaries(roomSummaryQueryParams {
            this.roomId = QueryStringValue.Equals(spaceId)
            this.memberships = listOf(Membership.JOIN)
        }).firstOrNull()

        assertEquals(2, spaceSummaryBobPov?.spaceChildren?.size ?: -1, "Unexpected number of children")

        commonTestHelper.signOutAndClose(aliceSession)
        commonTestHelper.signOutAndClose(bobSession)
    }
}
