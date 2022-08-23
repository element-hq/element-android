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

import android.util.Log
import androidx.lifecycle.Observer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesAllowEntry
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.RestrictedRoomPreset
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SpaceHierarchyTest : InstrumentedTest {

    @Test
    fun createCanonicalChildRelation() = runSessionTest(context()) { commonTestHelper ->

        val session = commonTestHelper.createAccount("John", SessionTestParams(true))
        val spaceName = "My Space"
        val topic = "A public space for test"
        var spaceId = ""
        commonTestHelper.runBlockingTest {
            spaceId = session.spaceService().createSpace(spaceName, topic, null, true)
        }

        val syncedSpace = session.spaceService().getSpace(spaceId)

        var roomId = ""
        commonTestHelper.runBlockingTest {
            roomId = session.roomService().createRoom(CreateRoomParams().apply { name = "General" })
        }

        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

        commonTestHelper.runBlockingTest {
            syncedSpace!!.addChildren(roomId, viaServers, null, true)
        }

        commonTestHelper.runBlockingTest {
            session.spaceService().setSpaceParent(roomId, spaceId, true, viaServers)
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val parents = session.getRoom(roomId)?.roomSummary()?.spaceParents
                val canonicalParents = session.getRoom(roomId)?.roomSummary()?.spaceParents?.filter { it.canonical == true }
                parents?.forEach {
                    Log.d("## TEST", "parent : $it")
                }
                parents?.size == 1 &&
                        parents.first().roomSummary?.name == spaceName &&
                        canonicalParents?.size == 1 &&
                        canonicalParents.first().roomSummary?.name == spaceName
            }
        }
    }

//    @Test
//    fun testCreateChildRelations() {
//        val session = commonTestHelper.createAccount("Jhon", SessionTestParams(true))
//        val spaceName = "My Space"
//        val topic = "A public space for test"
//        Log.d("## TEST", "Before")
//
//        var spaceId = ""
//        commonTestHelper.waitWithLatch {
//            GlobalScope.launch {
//                spaceId = session.spaceService().createSpace(spaceName, topic, null, true)
//                it.countDown()
//            }
//        }
//
//        Log.d("## TEST", "created space $spaceId ${Thread.currentThread()}")
//        val syncedSpace = session.spaceService().getSpace(spaceId)
//
//        val children = listOf("General" to true /*canonical*/, "Random" to false)
//
// //        val roomIdList = children.map {
// //            runBlocking {
// //                session.createRoom(CreateRoomParams().apply { name = it.first })
// //            } to it.second
// //        }
//        val roomIdList = mutableListOf<Pair<String, Boolean>>()
//        commonTestHelper.waitWithLatch {
//            GlobalScope.launch {
//                children.forEach {
//                    val rID = session.createRoom(CreateRoomParams().apply { name = it.first })
//                    roomIdList.add(rID to it.second)
//                }
//                it.countDown()
//            }
//        }
//
//        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
//
//        commonTestHelper.waitWithLatch {
//            GlobalScope.launch {
//                roomIdList.forEach { entry ->
//                    syncedSpace!!.addChildren(entry.first, viaServers, null, true)
//                }
//                it.countDown()
//            }
//        }
//
//        commonTestHelper.waitWithLatch {
//            GlobalScope.launch {
//                roomIdList.forEach {
//                    session.spaceService().setSpaceParent(it.first, spaceId, it.second, viaServers)
//                }
//                it.countDown()
//            }
//        }
//
//        roomIdList.forEach {
//            val parents = session.getRoom(it.first)?.roomSummary()?.spaceParents
//            val canonicalParents = session.getRoom(it.first)?.roomSummary()?.spaceParents?.filter { it.canonical == true }
//
//            assertNotNull(parents)
//            assertEquals("Unexpected number of parent", 1, parents!!.size)
//            assertEquals("Unexpected parent name", spaceName, parents.first().roomSummary?.name)
//            assertEquals("Parent of ${it.first} should be canonical ${it.second}", if (it.second) 1 else 0, canonicalParents?.size ?: 0)
//        }
//    }

    @Test
    fun testFilteringBySpace() = runSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(
                commonTestHelper,
                session, "SpaceA",
                listOf(
                        Triple("A1", true /*auto-join*/, true/*canonical*/),
                        Triple("A2", true, true)
                )
        )

        /* val spaceBInfo = */ createPublicSpace(
            commonTestHelper,
            session, "SpaceB",
            listOf(
                    Triple("B1", true /*auto-join*/, true/*canonical*/),
                    Triple("B2", true, true),
                    Triple("B3", true, true)
            )
    )

        val spaceCInfo = createPublicSpace(
                commonTestHelper,
                session, "SpaceC",
                listOf(
                        Triple("C1", true /*auto-join*/, true/*canonical*/),
                        Triple("C2", true, true)
                )
        )

        // add C as a subspace of A
        val spaceA = session.spaceService().getSpace(spaceAInfo.spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
        commonTestHelper.runBlockingTest {
            spaceA!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
            session.spaceService().setSpaceParent(spaceCInfo.spaceId, spaceAInfo.spaceId, true, viaServers)
        }

        // Create orphan rooms

        var orphan1 = ""
        commonTestHelper.runBlockingTest {
            orphan1 = session.roomService().createRoom(CreateRoomParams().apply { name = "O1" })
        }

        var orphan2 = ""
        commonTestHelper.runBlockingTest {
            orphan2 = session.roomService().createRoom(CreateRoomParams().apply { name = "O2" })
        }

        val allRooms = session.roomService().getRoomSummaries(roomSummaryQueryParams { excludeType = listOf(RoomType.SPACE) })

        assertEquals("Unexpected number of rooms", 9, allRooms.size)

        val orphans = session.roomService().getFlattenRoomSummaryChildrenOf(null)

        assertEquals("Unexpected number of orphan rooms", 2, orphans.size)
        assertTrue("O1 should be an orphan", orphans.any { it.roomId == orphan1 })
        assertTrue("O2 should be an orphan ${orphans.map { it.name }}", orphans.any { it.roomId == orphan2 })

        val aChildren = session.roomService().getFlattenRoomSummaryChildrenOf(spaceAInfo.spaceId)

        assertEquals("Unexpected number of flatten child rooms", 4, aChildren.size)
        assertTrue("A1 should be a child of A", aChildren.any { it.name == "A1" })
        assertTrue("A2 should be a child of A", aChildren.any { it.name == "A2" })
        assertTrue("CA should be a grand child of A", aChildren.any { it.name == "C1" })
        assertTrue("A1 should be a grand child of A", aChildren.any { it.name == "C2" })

        // Add a non canonical child and check that it does not appear as orphan
        commonTestHelper.runBlockingTest {
            val a3 = session.roomService().createRoom(CreateRoomParams().apply { name = "A3" })
            spaceA!!.addChildren(a3, viaServers, null, false)
        }

        Thread.sleep(6_000)
        val orphansUpdate = session.roomService().getRoomSummaries(roomSummaryQueryParams {
            spaceFilter = SpaceFilter.OrphanRooms
        })
        assertEquals("Unexpected number of orphan rooms ${orphansUpdate.map { it.name }}", 2, orphansUpdate.size)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun testBreakCycle() = runSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(
                commonTestHelper,
                session, "SpaceA",
                listOf(
                        Triple("A1", true /*auto-join*/, true/*canonical*/),
                        Triple("A2", true, true)
                )
        )

        val spaceCInfo = createPublicSpace(
                commonTestHelper,
                session, "SpaceC",
                listOf(
                        Triple("C1", true /*auto-join*/, true/*canonical*/),
                        Triple("C2", true, true)
                )
        )

        // add C as a subspace of A
        val spaceA = session.spaceService().getSpace(spaceAInfo.spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
        commonTestHelper.runBlockingTest {
            spaceA!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
            session.spaceService().setSpaceParent(spaceCInfo.spaceId, spaceAInfo.spaceId, true, viaServers)
        }

        // add back A as subspace of C
        commonTestHelper.runBlockingTest {
            val spaceC = session.spaceService().getSpace(spaceCInfo.spaceId)
            spaceC!!.addChildren(spaceAInfo.spaceId, viaServers, null, true)
        }

        // A -> C -> A

        val aChildren = session.roomService().getFlattenRoomSummaryChildrenOf(spaceAInfo.spaceId)

        assertEquals("Unexpected number of flatten child rooms ${aChildren.map { it.name }}", 4, aChildren.size)
        assertTrue("A1 should be a child of A", aChildren.any { it.name == "A1" })
        assertTrue("A2 should be a child of A", aChildren.any { it.name == "A2" })
        assertTrue("CA should be a grand child of A", aChildren.any { it.name == "C1" })
        assertTrue("A1 should be a grand child of A", aChildren.any { it.name == "C2" })
    }

    @Test
    fun testLiveFlatChildren() = runSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(
                commonTestHelper,
                session,
                "SpaceA",
                listOf(
                        Triple("A1", true /*auto-join*/, true/*canonical*/),
                        Triple("A2", true, true)
                )
        )

        val spaceBInfo = createPublicSpace(
                commonTestHelper,
                session,
                "SpaceB",
                listOf(
                        Triple("B1", true /*auto-join*/, true/*canonical*/),
                        Triple("B2", true, true),
                        Triple("B3", true, true)
                )
        )

        // add B as a subspace of A
        val spaceA = session.spaceService().getSpace(spaceAInfo.spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
        commonTestHelper.runBlockingTest {
            spaceA!!.addChildren(spaceBInfo.spaceId, viaServers, null, true)
            session.spaceService().setSpaceParent(spaceBInfo.spaceId, spaceAInfo.spaceId, true, viaServers)
        }

        val spaceCInfo = createPublicSpace(
                commonTestHelper,
                session,
                "SpaceC",
                listOf(
                        Triple("C1", true /*auto-join*/, true/*canonical*/),
                        Triple("C2", true, true)
                )
        )

        commonTestHelper.waitWithLatch { latch ->

            val flatAChildren = session.roomService().getFlattenRoomSummaryChildrenOfLive(spaceAInfo.spaceId)
            val childObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(children: List<RoomSummary>?) {
//                    Log.d("## TEST", "Space A flat children update : ${children?.map { it.name }}")
                    System.out.println("## TEST | Space A flat children update : ${children?.map { it.name }}")
                    if (children?.any { it.name == "C1" } == true && children.any { it.name == "C2" }) {
                        // B1 has been added live!
                        latch.countDown()
                        flatAChildren.removeObserver(this)
                    }
                }
            }

            flatAChildren.observeForever(childObserver)

            // add C as subspace of B
            val spaceB = session.spaceService().getSpace(spaceBInfo.spaceId)
            spaceB!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)

            // C1 and C2 should be in flatten child of A now
        }

        // Test part one of the rooms

        val bRoomId = spaceBInfo.roomIds.first()

        commonTestHelper.waitWithLatch { latch ->
            val flatAChildren = session.roomService().getFlattenRoomSummaryChildrenOfLive(spaceAInfo.spaceId)
            val childObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(children: List<RoomSummary>?) {
                    System.out.println("## TEST | Space A flat children update : ${children?.map { it.name }}")
                    if (children?.any { it.roomId == bRoomId } == false) {
                        // B1 has been added live!
                        latch.countDown()
                        flatAChildren.removeObserver(this)
                    }
                }
            }

            // The room should have disapear from flat children
            flatAChildren.observeForever(childObserver)
            // part from b room
            session.roomService().leaveRoom(bRoomId)
        }
        commonTestHelper.signOutAndClose(session)
    }

    data class TestSpaceCreationResult(
            val spaceId: String,
            val roomIds: List<String>
    )

    private fun createPublicSpace(
            commonTestHelper: CommonTestHelper,
            session: Session,
            spaceName: String,
            childInfo: List<Triple<String, Boolean, Boolean?>>
            /** Name, auto-join, canonical*/
    ): TestSpaceCreationResult {
        var spaceId = ""
        var roomIds: List<String> = emptyList()
        commonTestHelper.runBlockingTest {
            spaceId = session.spaceService().createSpace(spaceName, "Test Topic", null, true)
            val syncedSpace = session.spaceService().getSpace(spaceId)
            val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

            roomIds = childInfo.map { entry ->
                session.roomService().createRoom(CreateRoomParams().apply { name = entry.first })
            }
            roomIds.forEachIndexed { index, roomId ->
                syncedSpace!!.addChildren(roomId, viaServers, null, childInfo[index].second)
                val canonical = childInfo[index].third
                if (canonical != null) {
                    session.spaceService().setSpaceParent(roomId, spaceId, canonical, viaServers)
                }
            }
        }
        return TestSpaceCreationResult(spaceId, roomIds)
    }

    private fun createPrivateSpace(
            commonTestHelper: CommonTestHelper,
            session: Session,
            spaceName: String,
            childInfo: List<Triple<String, Boolean, Boolean?>>
            /** Name, auto-join, canonical*/
    ): TestSpaceCreationResult {
        var spaceId = ""
        var roomIds: List<String> = emptyList()
        commonTestHelper.runBlockingTest {
            spaceId = session.spaceService().createSpace(spaceName, "My Private Space", null, false)
            val syncedSpace = session.spaceService().getSpace(spaceId)
            val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
            roomIds =
                    childInfo.map { entry ->
                        val homeServerCapabilities = session
                                .homeServerCapabilitiesService()
                                .getHomeServerCapabilities()
                        session.roomService().createRoom(CreateRoomParams().apply {
                            name = entry.first
                            this.featurePreset = RestrictedRoomPreset(
                                    homeServerCapabilities,
                                    listOf(
                                            RoomJoinRulesAllowEntry.restrictedToRoom(spaceId)
                                    )
                            )
                        })
                    }
            roomIds.forEachIndexed { index, roomId ->
                syncedSpace!!.addChildren(roomId, viaServers, null, childInfo[index].second)
                val canonical = childInfo[index].third
                if (canonical != null) {
                    session.spaceService().setSpaceParent(roomId, spaceId, canonical, viaServers)
                }
            }
        }
        return TestSpaceCreationResult(spaceId, roomIds)
    }

    @Test
    fun testRootSpaces() = runSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        /* val spaceAInfo = */ createPublicSpace(
            commonTestHelper,
            session, "SpaceA",
            listOf(
                    Triple("A1", true /*auto-join*/, true/*canonical*/),
                    Triple("A2", true, true)
            )
    )

        val spaceBInfo = createPublicSpace(
                commonTestHelper,
                session, "SpaceB",
                listOf(
                        Triple("B1", true /*auto-join*/, true/*canonical*/),
                        Triple("B2", true, true),
                        Triple("B3", true, true)
                )
        )

        val spaceCInfo = createPublicSpace(
                commonTestHelper,
                session, "SpaceC",
                listOf(
                        Triple("C1", true /*auto-join*/, true/*canonical*/),
                        Triple("C2", true, true)
                )
        )

        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

        // add C as subspace of B
        runBlocking {
            val spaceB = session.spaceService().getSpace(spaceBInfo.spaceId)
            spaceB!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
        }

//        Thread.sleep(4_000)
        // + A
        //   a1, a2
        // + B
        //  b1, b2, b3
        //   + C
        //     + c1, c2

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val rootSpaces = commonTestHelper.runBlockingTest { session.spaceService().getRootSpaceSummaries() }
                rootSpaces.size == 2
            }
        }
    }

    @Test
    fun testParentRelation() = runSessionTest(context()) { commonTestHelper ->
        val aliceSession = commonTestHelper.createAccount("Alice", SessionTestParams(true))
        val bobSession = commonTestHelper.createAccount("Bib", SessionTestParams(true))

        val spaceAInfo = createPrivateSpace(
                commonTestHelper,
                aliceSession, "Private Space A",
                listOf(
                        Triple("General", true /*suggested*/, true/*canonical*/),
                        Triple("Random", true, true)
                )
        )

        commonTestHelper.runBlockingTest {
            aliceSession.getRoom(spaceAInfo.spaceId)!!.membershipService().invite(bobSession.myUserId, null)
        }

        commonTestHelper.runBlockingTest {
            bobSession.roomService().joinRoom(spaceAInfo.spaceId, null, emptyList())
        }

        var bobRoomId = ""
        commonTestHelper.runBlockingTest {
            bobRoomId = bobSession.roomService().createRoom(CreateRoomParams().apply { name = "A Bob Room" })
            bobSession.getRoom(bobRoomId)!!.membershipService().invite(aliceSession.myUserId)
        }

        commonTestHelper.runBlockingTest {
            aliceSession.roomService().joinRoom(bobRoomId)
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession.getRoomSummary(bobRoomId)?.membership?.isActive() == true
            }
        }

        commonTestHelper.runBlockingTest {
            bobSession.spaceService().setSpaceParent(bobRoomId, spaceAInfo.spaceId, false, listOf(bobSession.sessionParams.homeServerHost ?: ""))
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val stateEvent = aliceSession.getRoom(bobRoomId)!!.getStateEvent(EventType.STATE_SPACE_PARENT, QueryStringValue.Equals(spaceAInfo.spaceId))
                stateEvent != null
            }
        }

        // This should be an invalid space parent relation, because no opposite child and bob is not admin of the space
        commonTestHelper.runBlockingTest {
            // we can see the state event
            // but it is not valid and room is not in hierarchy
            assertTrue("Bob Room should not be listed as a child of the space", aliceSession.getRoomSummary(bobRoomId)?.flattenParentIds?.isEmpty() == true)
        }

        // Let's now try to make alice admin of the room

        commonTestHelper.waitWithLatch {
            val room = bobSession.getRoom(bobRoomId)!!
            val currentPLContent = room
                    .getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                    ?.content
                    .toModel<PowerLevelsContent>()

            val newPowerLevelsContent = currentPLContent
                    ?.setUserPowerLevel(aliceSession.myUserId, Role.Admin.value)
                    ?.toContent()

            room.stateService().sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent!!)
            it.countDown()
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val powerLevelsHelper = aliceSession.getRoom(bobRoomId)!!
                        .getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                        ?.content
                        ?.toModel<PowerLevelsContent>()
                        ?.let { PowerLevelsHelper(it) }
                powerLevelsHelper!!.isUserAllowedToSend(aliceSession.myUserId, true, EventType.STATE_SPACE_PARENT)
            }
        }

        commonTestHelper.waitWithLatch {
            aliceSession.spaceService().setSpaceParent(bobRoomId, spaceAInfo.spaceId, false, listOf(bobSession.sessionParams.homeServerHost ?: ""))
            it.countDown()
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                bobSession.getRoomSummary(bobRoomId)?.flattenParentIds?.contains(spaceAInfo.spaceId) == true
            }
        }
    }

    @Test
    fun testDirectParentNames() = runSessionTest(context()) { commonTestHelper ->
        val aliceSession = commonTestHelper.createAccount("Alice", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(
                commonTestHelper,
                aliceSession, "SpaceA",
                listOf(
                        Triple("A1", true /*auto-join*/, true/*canonical*/),
                        Triple("A2", true, true)
                )
        )

        val spaceBInfo = createPublicSpace(
                commonTestHelper,
                aliceSession, "SpaceB",
                listOf(
                        Triple("B1", true /*auto-join*/, true/*canonical*/),
                        Triple("B2", true, true),
                        Triple("B3", true, true)
                )
        )

        // also add B1 in space A

        val B1roomId = spaceBInfo.roomIds.first()
        val viaServers = listOf(aliceSession.sessionParams.homeServerHost ?: "")

        val spaceA = aliceSession.spaceService().getSpace(spaceAInfo.spaceId)
        val spaceB = aliceSession.spaceService().getSpace(spaceBInfo.spaceId)
        commonTestHelper.runBlockingTest {
            spaceA!!.addChildren(B1roomId, viaServers, null, true)
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val roomSummary = aliceSession.getRoomSummary(B1roomId)
                roomSummary != null &&
                        roomSummary.directParentNames.size == 2 &&
                        roomSummary.directParentNames.contains(spaceA!!.spaceSummary()!!.name) &&
                        roomSummary.directParentNames.contains(spaceB!!.spaceSummary()!!.name)
            }
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val roomSummary = aliceSession.getRoomSummary(spaceAInfo.roomIds.first())
                roomSummary != null &&
                        roomSummary.directParentNames.size == 1 &&
                        roomSummary.directParentNames.contains(spaceA!!.spaceSummary()!!.name)
            }
        }

        val newAName = "FooBar"
        commonTestHelper.runBlockingTest {
            spaceA!!.asRoom().stateService().updateName(newAName)
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val roomSummary = aliceSession.getRoomSummary(B1roomId)
                roomSummary != null &&
                        roomSummary.directParentNames.size == 2 &&
                        roomSummary.directParentNames.contains(newAName) &&
                        roomSummary.directParentNames.contains(spaceB!!.spaceSummary()!!.name)
            }
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val roomSummary = aliceSession.getRoomSummary(spaceAInfo.roomIds.first())
                roomSummary != null &&
                        roomSummary.directParentNames.size == 1 &&
                        roomSummary.directParentNames.contains(newAName)
            }
        }
    }
}
