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

package org.matrix.android.sdk.internal.util

import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.MatrixTest
import org.matrix.android.sdk.internal.session.room.summary.Graph
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.JVM)
class GraphUtilsTest : MatrixTest {

    @Test
    fun testCreateGraph() {
        val graph = Graph()

        graph.addEdge("E", "C")
        graph.addEdge("B", "A")
        graph.addEdge("C", "A")
        graph.addEdge("D", "C")
        graph.addEdge("E", "D")

        graph.getOrCreateNode("F")

        System.out.println(graph.toString())

        val backEdges = graph.findBackwardEdges(graph.getOrCreateNode("E"))

        assertTrue(backEdges.isEmpty(), "There should not be any cycle in this graphs")
    }

    @Test
    fun testCycleGraph() {
        val graph = Graph()

        graph.addEdge("E", "C")
        graph.addEdge("B", "A")
        graph.addEdge("C", "A")
        graph.addEdge("D", "C")
        graph.addEdge("E", "D")

        graph.getOrCreateNode("F")

        // adding loops
        graph.addEdge("C", "E")
        graph.addEdge("B", "B")

        System.out.println(graph.toString())

        val backEdges = graph.findBackwardEdges(graph.getOrCreateNode("E"))
        System.out.println(backEdges.joinToString(" | ") { "${it.source.name} -> ${it.destination.name}" })

        assertTrue(backEdges.size == 2, "There should be 2 backward edges not ${backEdges.size}")

        val edge1 = backEdges.find { it.source.name == "C" }
        assertNotNull(edge1, "There should be a back edge from C")
        assertEquals("E", edge1.destination.name, "There should be a back edge C -> E")

        val edge2 = backEdges.find { it.source.name == "B" }
        assertNotNull(edge2, "There should be a back edge from B")
        assertEquals("B", edge2.destination.name, "There should be a back edge C -> C")

        // clean the graph
        val acyclicGraph = graph.withoutEdges(backEdges)
        System.out.println(acyclicGraph.toString())

        assertTrue(acyclicGraph.findBackwardEdges(acyclicGraph.getOrCreateNode("E")).isEmpty(), "There should be no backward edges")

        val flatten = acyclicGraph.flattenDestination()

        assertTrue(flatten[acyclicGraph.getOrCreateNode("A")]!!.isEmpty())

        val flattenParentsB = flatten[acyclicGraph.getOrCreateNode("B")]
        assertTrue(flattenParentsB!!.size == 1)
        assertTrue(flattenParentsB.contains(acyclicGraph.getOrCreateNode("A")))

        val flattenParentsE = flatten[acyclicGraph.getOrCreateNode("E")]
        assertTrue(flattenParentsE!!.size == 3)
        assertTrue(flattenParentsE.contains(acyclicGraph.getOrCreateNode("A")))
        assertTrue(flattenParentsE.contains(acyclicGraph.getOrCreateNode("C")))
        assertTrue(flattenParentsE.contains(acyclicGraph.getOrCreateNode("D")))

//        System.out.println(
//                buildString {
//                    flatten.entries.forEach {
//                        append("${it.key.name}: [")
//                        append(it.value.joinToString(",") { it.name })
//                        append("]\n")
//                    }
//                }
//        )
    }
}
