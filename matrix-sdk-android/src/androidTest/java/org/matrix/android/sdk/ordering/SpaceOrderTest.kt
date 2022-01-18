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

package org.matrix.android.sdk.ordering

import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.Test
import org.matrix.android.sdk.api.session.space.SpaceOrderUtils

class SpaceOrderTest {

    @Test
    fun testOrderBetweenNodesWithOrder() {
        val orderedSpaces = listOf(
                "roomId1" to "a",
                "roomId2" to "m",
                "roomId3" to "z"
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId1", 1)

        Assert.assertTrue("Only one order should be changed", orderCommand.size == 1)
        Assert.assertTrue("Moved space order should change", orderCommand.first().spaceId == "roomId1")

        Assert.assertTrue("m" < orderCommand[0].order)
        Assert.assertTrue(orderCommand[0].order < "z")
    }

    @Test
    fun testMoveLastBetweenNodesWithOrder() {
        val orderedSpaces = listOf(
                "roomId1" to "a",
                "roomId2" to "m",
                "roomId3" to "z"
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId1", 2)

        Assert.assertTrue("Only one order should be changed", orderCommand.size == 1)
        Assert.assertTrue("Moved space order should change", orderCommand.first().spaceId == "roomId1")

        Assert.assertTrue("z" < orderCommand[0].order)
    }

    @Test
    fun testMoveUpNoOrder() {
        val orderedSpaces = listOf(
                "roomId1" to null,
                "roomId2" to null,
                "roomId3" to null
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId1", 1)

        Assert.assertTrue("2 orders change should be needed", orderCommand.size == 2)

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId2", reOrdered[0].first)
        Assert.assertEquals("roomId1", reOrdered[1].first)
        Assert.assertEquals("roomId3", reOrdered[2].first)
    }

    @Test
    fun testMoveUpNotEnoughSpace() {
        val orderedSpaces = listOf(
                "roomId1" to "a",
                "roomId2" to "j",
                "roomId3" to "k"
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId1", 1)

        Assert.assertTrue("more orders change should be needed", orderCommand.size > 1)

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId2", reOrdered[0].first)
        Assert.assertEquals("roomId1", reOrdered[1].first)
        Assert.assertEquals("roomId3", reOrdered[2].first)
    }

    @Test
    fun testMoveEndNoOrder() {
        val orderedSpaces = listOf(
                "roomId1" to null,
                "roomId2" to null,
                "roomId3" to null
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId1", 2)

        // Actually 2 could be enough... as it's last it can stays with null
        Assert.assertEquals("3 orders change should be needed", 3, orderCommand.size)

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId2", reOrdered[0].first)
        Assert.assertEquals("roomId3", reOrdered[1].first)
        Assert.assertEquals("roomId1", reOrdered[2].first)
    }

    @Test
    fun testMoveUpBiggerOrder() {
        val orderedSpaces = listOf(
                "roomId1" to "aaaa",
                "roomId2" to "ffff",
                "roomId3" to "pppp",
                "roomId4" to null,
                "roomId5" to null,
                "roomId6" to null
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId2", 3)

        // Actually 2 could be enough... as it's last it can stays with null
        Assert.assertEquals("3 orders change should be needed", 3, orderCommand.size)

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId1", reOrdered[0].first)
        Assert.assertEquals("roomId3", reOrdered[1].first)
        Assert.assertEquals("roomId4", reOrdered[2].first)
        Assert.assertEquals("roomId5", reOrdered[3].first)
        Assert.assertEquals("roomId2", reOrdered[4].first)
        Assert.assertEquals("roomId6", reOrdered[5].first)
    }

    @Test
    fun testMoveDownBetweenNodesWithOrder() {
        val orderedSpaces = listOf(
                "roomId1" to "a",
                "roomId2" to "m",
                "roomId3" to "z"
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId3", -1)

        Assert.assertTrue("Only one order should be changed", orderCommand.size == 1)
        Assert.assertTrue("Moved space order should change", orderCommand.first().spaceId == "roomId3")

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId1", reOrdered[0].first)
        Assert.assertEquals("roomId3", reOrdered[1].first)
        Assert.assertEquals("roomId2", reOrdered[2].first)
    }

    @Test
    fun testMoveDownNoOrder() {
        val orderedSpaces = listOf(
                "roomId1" to null,
                "roomId2" to null,
                "roomId3" to null
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId3", -1)

        Assert.assertTrue("2 orders change should be needed", orderCommand.size == 2)

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId1", reOrdered[0].first)
        Assert.assertEquals("roomId3", reOrdered[1].first)
        Assert.assertEquals("roomId2", reOrdered[2].first)
    }

    @Test
    fun testMoveDownBiggerOrder() {
        val orderedSpaces = listOf(
                "roomId1" to "aaaa",
                "roomId2" to "ffff",
                "roomId3" to "pppp",
                "roomId4" to null,
                "roomId5" to null,
                "roomId6" to null
        ).assertSpaceOrdered()

        val orderCommand = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId5", -4)

        Assert.assertEquals("1 order change should be needed", 1, orderCommand.size)

        val reOrdered = reOrderWithCommands(orderedSpaces, orderCommand)

        Assert.assertEquals("roomId5", reOrdered[0].first)
        Assert.assertEquals("roomId1", reOrdered[1].first)
        Assert.assertEquals("roomId2", reOrdered[2].first)
        Assert.assertEquals("roomId3", reOrdered[3].first)
        Assert.assertEquals("roomId4", reOrdered[4].first)
        Assert.assertEquals("roomId6", reOrdered[5].first)
    }

    @Test
    fun testMultipleMoveOrder() {
        val orderedSpaces = listOf(
                "roomId1" to null,
                "roomId2" to null,
                "roomId3" to null,
                "roomId4" to null,
                "roomId5" to null,
                "roomId6" to null
        ).assertSpaceOrdered()

        // move 5 to Top
        val fiveToTop = SpaceOrderUtils.orderCommandsForMove(orderedSpaces, "roomId5", -4)

        val fiveTopReOrder = reOrderWithCommands(orderedSpaces, fiveToTop)

        // now move 4 to second
        val orderCommand = SpaceOrderUtils.orderCommandsForMove(fiveTopReOrder, "roomId4", -3)

        val reOrdered = reOrderWithCommands(fiveTopReOrder, orderCommand)
        // second order should cost 1 re-order
        Assert.assertEquals(1, orderCommand.size)

        Assert.assertEquals("roomId5", reOrdered[0].first)
        Assert.assertEquals("roomId4", reOrdered[1].first)
        Assert.assertEquals("roomId1", reOrdered[2].first)
        Assert.assertEquals("roomId2", reOrdered[3].first)
        Assert.assertEquals("roomId3", reOrdered[4].first)
        Assert.assertEquals("roomId6", reOrdered[5].first)
    }

    @Test
    fun testComparator() {
        listOf(
                "roomId2" to "a",
                "roomId1" to "b",
                "roomId3" to null,
                "roomId4" to null
        ).assertSpaceOrdered()
    }

    private fun reOrderWithCommands(orderedSpaces: List<Pair<String, String?>>, orderCommand: List<SpaceOrderUtils.SpaceReOrderCommand>) =
            orderedSpaces.map { orderInfo ->
                orderInfo.first to (orderCommand.find { it.spaceId == orderInfo.first }?.order ?: orderInfo.second)
            }
                    .sortedWith(testSpaceComparator)

    private fun List<Pair<String, String?>>.assertSpaceOrdered(): List<Pair<String, String?>> {
        assertEquals(this, this.sortedWith(testSpaceComparator))
        return this
    }

    private val testSpaceComparator = compareBy<Pair<String, String?>, String?>(nullsLast()) { it.second }.thenBy { it.first }
}
