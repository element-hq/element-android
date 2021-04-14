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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SpaceHierarchyTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    @Test
    fun createCanonicalChildRelation() {
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))
        val spaceName = "My Space"
        val topic = "A public space for test"
        val spaceId: String
        runBlocking {
            spaceId = session.spaceService().createSpace(spaceName, topic, null, true)
            // wait a bit to let the summary update it self :/
            delay(400)
        }

        val syncedSpace = session.spaceService().getSpace(spaceId)

        val roomId = runBlocking {
            session.createRoom(CreateRoomParams().apply { name = "General" })
        }

        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

        runBlocking {
            syncedSpace!!.addChildren(roomId, viaServers, null, true)
        }

        runBlocking {
            session.spaceService().setSpaceParent(roomId, spaceId, true, viaServers)
        }

        Thread.sleep(9000)

        val parents = session.getRoom(roomId)?.roomSummary()?.spaceParents
        val canonicalParents = session.getRoom(roomId)?.roomSummary()?.spaceParents?.filter { it.canonical == true }

        parents?.forEach {
            Log.d("## TEST", "parent : $it")
        }

        assertNotNull(parents)
        assertEquals(1, parents.size)
        assertEquals(spaceName, parents.first().roomSummary?.name)

        assertNotNull(canonicalParents)
        assertEquals(1, canonicalParents.size)
        assertEquals(spaceName, canonicalParents.first().roomSummary?.name)
    }

    @Test
    fun testCreateChildRelations() {
        val session = commonTestHelper.createAccount("Jhon", SessionTestParams(true))
        val spaceName = "My Space"
        val topic = "A public space for test"
        Log.d("## TEST", "Before")
        val spaceId = runBlocking {
            session.spaceService().createSpace(spaceName, topic, null, true)
        }

        Log.d("## TEST", "created space $spaceId ${Thread.currentThread()}")
        val syncedSpace = session.spaceService().getSpace(spaceId)

        val children = listOf("General" to true /*canonical*/, "Random" to false)

        val roomIdList = children.map {
            runBlocking {
                session.createRoom(CreateRoomParams().apply { name = it.first })
            } to it.second
        }

        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

        runBlocking {
            roomIdList.forEach { entry ->
                syncedSpace!!.addChildren(entry.first, viaServers, null, true)
            }
        }

        runBlocking {
            roomIdList.forEach {
                session.spaceService().setSpaceParent(it.first, spaceId, it.second, viaServers)
            }
            delay(400)
        }

        roomIdList.forEach {
            val parents = session.getRoom(it.first)?.roomSummary()?.spaceParents
            val canonicalParents = session.getRoom(it.first)?.roomSummary()?.spaceParents?.filter { it.canonical == true }

            assertNotNull(parents)
            assertEquals(1, parents.size, "Unexpected number of parent")
            assertEquals(spaceName, parents.first().roomSummary?.name, "Unexpected parent name ")
            assertEquals(if (it.second) 1 else 0, canonicalParents?.size ?: 0, "Parent of ${it.first} should be canonical ${it.second}")
        }
    }

    @Test
    fun testFilteringBySpace() {
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(session, "SpaceA", listOf(
                Triple("A1", true /*auto-join*/, true/*canonical*/),
                Triple("A2", true, true)
        ))

        val spaceBInfo = createPublicSpace(session, "SpaceB", listOf(
                Triple("B1", true /*auto-join*/, true/*canonical*/),
                Triple("B2", true, true),
                Triple("B3", true, true)
        ))

        val spaceCInfo = createPublicSpace(session, "SpaceC", listOf(
                Triple("C1", true /*auto-join*/, true/*canonical*/),
                Triple("C2", true, true)
        ))

        // add C as a subspace of A
        val spaceA = session.spaceService().getSpace(spaceAInfo.spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
        runBlocking {
            spaceA!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
            session.spaceService().setSpaceParent(spaceCInfo.spaceId, spaceAInfo.spaceId, true, viaServers)
        }

        // Create orphan rooms

        val orphan1 = runBlocking {
            session.createRoom(CreateRoomParams().apply { name = "O1" })
        }
        val orphan2 = runBlocking {
            session.createRoom(CreateRoomParams().apply { name = "O2" })
        }

        val allRooms = session.getRoomSummaries(roomSummaryQueryParams { excludeType = listOf(RoomType.SPACE) })

        assertEquals(9, allRooms.size, "Unexpected number of rooms")

        val orphans = session.getFlattenRoomSummaryChildrenOf(null)

        assertEquals(2, orphans.size, "Unexpected number of orphan rooms")
        assertTrue(orphans.indexOfFirst { it.roomId == orphan1 } != -1, "O1 should be an orphan")
        assertTrue(orphans.indexOfFirst { it.roomId == orphan2 } != -1, "O2 should be an orphan ${orphans.map { it.name }}")

        val aChildren = session.getFlattenRoomSummaryChildrenOf(spaceAInfo.spaceId)

        assertEquals(4, aChildren.size, "Unexpected number of flatten child rooms")
        assertTrue(aChildren.indexOfFirst { it.name == "A1" } != -1, "A1 should be a child of A")
        assertTrue(aChildren.indexOfFirst { it.name == "A2" } != -1, "A2 should be a child of A")
        assertTrue(aChildren.indexOfFirst { it.name == "C1" } != -1, "CA should be a grand child of A")
        assertTrue(aChildren.indexOfFirst { it.name == "C2" } != -1, "A1 should be a grand child of A")

        // Add a non canonical child and check that it does not appear as orphan
        val a3 = runBlocking {
            session.createRoom(CreateRoomParams().apply { name = "A3" })
        }
        runBlocking {
            spaceA!!.addChildren(a3, viaServers, null, false)
            delay(400)
            // here we do not set the parent!!
        }

        val orphansUpdate = session.getFlattenRoomSummaryChildrenOf(null)
        assertEquals(2, orphansUpdate.size, "Unexpected number of orphan rooms ${orphansUpdate.map { it.name }}")
    }

    @Test
    fun testBreakCycle() {
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(session, "SpaceA", listOf(
                Triple("A1", true /*auto-join*/, true/*canonical*/),
                Triple("A2", true, true)
        ))

        val spaceCInfo = createPublicSpace(session, "SpaceC", listOf(
                Triple("C1", true /*auto-join*/, true/*canonical*/),
                Triple("C2", true, true)
        ))

        // add C as a subspace of A
        val spaceA = session.spaceService().getSpace(spaceAInfo.spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
        runBlocking {
            spaceA!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
            session.spaceService().setSpaceParent(spaceCInfo.spaceId, spaceAInfo.spaceId, true, viaServers)
        }

        // add back A as subspace of C
        runBlocking {
            val spaceC = session.spaceService().getSpace(spaceCInfo.spaceId)
            spaceC!!.addChildren(spaceAInfo.spaceId, viaServers, null, true)
        }

        Thread.sleep(1000)

        // A -> C -> A

        val aChildren = session.getFlattenRoomSummaryChildrenOf(spaceAInfo.spaceId)

        assertEquals(4, aChildren.size, "Unexpected number of flatten child rooms ${aChildren.map { it.name }}")
        assertTrue(aChildren.indexOfFirst { it.name == "A1" } != -1, "A1 should be a child of A")
        assertTrue(aChildren.indexOfFirst { it.name == "A2" } != -1, "A2 should be a child of A")
        assertTrue(aChildren.indexOfFirst { it.name == "C1" } != -1, "CA should be a grand child of A")
        assertTrue(aChildren.indexOfFirst { it.name == "C2" } != -1, "A1 should be a grand child of A")
    }

    @Test
    fun testLiveFlatChildren() {
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(session, "SpaceA", listOf(
                Triple("A1", true /*auto-join*/, true/*canonical*/),
                Triple("A2", true, true)
        ))

        val spaceBInfo = createPublicSpace(session, "SpaceB", listOf(
                Triple("B1", true /*auto-join*/, true/*canonical*/),
                Triple("B2", true, true),
                Triple("B3", true, true)
        ))

        // add B as a subspace of A
        val spaceA = session.spaceService().getSpace(spaceAInfo.spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")
        runBlocking {
            spaceA!!.addChildren(spaceBInfo.spaceId, viaServers, null, true)
            session.spaceService().setSpaceParent(spaceBInfo.spaceId, spaceAInfo.spaceId, true, viaServers)
        }

        val flatAChildren = runBlocking(Dispatchers.Main) {
            session.getFlattenRoomSummaryChildrenOfLive(spaceAInfo.spaceId)
        }

        commonTestHelper.waitWithLatch { latch ->

            val childObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(children: List<RoomSummary>?) {
//                    Log.d("## TEST", "Space A flat children update : ${children?.map { it.name }}")
                    System.out.println("## TEST | Space A flat children update : ${children?.map { it.name }}")
                    if (children?.indexOfFirst { it.name == "C1" } != -1
                            && children?.indexOfFirst { it.name == "C2" } != -1
                    ) {
                        // B1 has been added live!
                        latch.countDown()
                        flatAChildren.removeObserver(this)
                    }
                }
            }

            val spaceCInfo = createPublicSpace(session, "SpaceC", listOf(
                    Triple("C1", true /*auto-join*/, true/*canonical*/),
                    Triple("C2", true, true)
            ))

            // add C as subspace of B
            runBlocking {
                val spaceB = session.spaceService().getSpace(spaceBInfo.spaceId)
                spaceB!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
            }

            // C1 and C2 should be in flatten child of A now

            GlobalScope.launch(Dispatchers.Main) { flatAChildren.observeForever(childObserver) }
        }

        // Test part one of the rooms

        val bRoomId = spaceBInfo.roomIds.first()
        val bRoom = session.getRoom(bRoomId)

        commonTestHelper.waitWithLatch { latch ->

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

            // part from b room
            runBlocking {
                bRoom!!.leave(null)
            }
            // The room should have disapear from flat children
            GlobalScope.launch(Dispatchers.Main) { flatAChildren.observeForever(childObserver) }
        }
    }

    data class TestSpaceCreationResult(
            val spaceId: String,
            val roomIds: List<String>
    )

    private fun createPublicSpace(session: Session,
                                  spaceName: String,
                                  childInfo: List<Triple<String, Boolean, Boolean?>>
            /** Name, auto-join, canonical*/
    ): TestSpaceCreationResult {
        val spaceId = runBlocking {
            session.spaceService().createSpace(spaceName, "Test Topic", null, true)
        }

        val syncedSpace = session.spaceService().getSpace(spaceId)
        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

        val roomIds =
                childInfo.map { entry ->
                    runBlocking {
                        session.createRoom(CreateRoomParams().apply { name = entry.first })
                    }
                }

        roomIds.forEachIndexed { index, roomId ->
            runBlocking {
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
    fun testRootSpaces() {
        val session = commonTestHelper.createAccount("John", SessionTestParams(true))

        val spaceAInfo = createPublicSpace(session, "SpaceA", listOf(
                Triple("A1", true /*auto-join*/, true/*canonical*/),
                Triple("A2", true, true)
        ))

        val spaceBInfo = createPublicSpace(session, "SpaceB", listOf(
                Triple("B1", true /*auto-join*/, true/*canonical*/),
                Triple("B2", true, true),
                Triple("B3", true, true)
        ))

        val spaceCInfo = createPublicSpace(session, "SpaceC", listOf(
                Triple("C1", true /*auto-join*/, true/*canonical*/),
                Triple("C2", true, true)
        ))

        val viaServers = listOf(session.sessionParams.homeServerHost ?: "")

        // add C as subspace of B
        runBlocking {
            val spaceB = session.spaceService().getSpace(spaceBInfo.spaceId)
            spaceB!!.addChildren(spaceCInfo.spaceId, viaServers, null, true)
        }

        Thread.sleep(2000)
        // + A
        //   a1, a2
        // + B
         //  b1, b2, b3
        //   + C
        //     + c1, c2

        val rootSpaces = session.spaceService().getRootSpaceSummaries()

        assertEquals(2, rootSpaces.size, "Unexpected number of root spaces ${rootSpaces.map { it.name }}")
    }
}
