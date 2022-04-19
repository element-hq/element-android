/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.summary

import java.util.LinkedList

internal data class GraphNode(
        val name: String
)

internal data class GraphEdge(
        val source: GraphNode,
        val destination: GraphNode
)

internal class Graph {

    private val adjacencyList: HashMap<GraphNode, ArrayList<GraphEdge>> = HashMap()

    fun getOrCreateNode(name: String): GraphNode {
        return adjacencyList.entries.firstOrNull { it.key.name == name }?.key
                ?: GraphNode(name).also {
                    adjacencyList[it] = ArrayList()
                }
    }

    fun addEdge(sourceName: String, destinationName: String) {
        val source = getOrCreateNode(sourceName)
        val destination = getOrCreateNode(destinationName)
        adjacencyList.getOrPut(source) { ArrayList() }.add(
                GraphEdge(source, destination)
        )
    }

    fun addEdge(source: GraphNode, destination: GraphNode) {
        adjacencyList.getOrPut(source) { ArrayList() }.add(
                GraphEdge(source, destination)
        )
    }

    fun edgesOf(node: GraphNode): List<GraphEdge> {
        return adjacencyList[node]?.toList() ?: emptyList()
    }

    fun withoutEdges(edgesToPrune: List<GraphEdge>): Graph {
        val output = Graph()
        this.adjacencyList.forEach { (vertex, edges) ->
            output.getOrCreateNode(vertex.name)
            edges.forEach {
                if (!edgesToPrune.contains(it)) {
                    // add it
                    output.addEdge(it.source, it.destination)
                }
            }
        }
        return output
    }

    /**
     * Depending on the chosen starting point the background edge might change
     */
    fun findBackwardEdges(startFrom: GraphNode? = null): List<GraphEdge> {
        val backwardEdges = mutableSetOf<GraphEdge>()
        val visited = mutableMapOf<GraphNode, Int>()
        val notVisited = -1
        val inPath = 0
        val completed = 1
        adjacencyList.keys.forEach {
            visited[it] = notVisited
        }
        val stack = LinkedList<GraphNode>()

        (startFrom ?: adjacencyList.entries.firstOrNull { visited[it.key] == notVisited }?.key)
                ?.let {
                    stack.push(it)
                    visited[it] = inPath
                }

        while (stack.isNotEmpty()) {
//            Timber.w("VAL: current stack: ${stack.reversed().joinToString { it.name }}")
            val vertex = stack.peek() ?: break
            // peek a path to follow
            var destination: GraphNode? = null
            edgesOf(vertex).forEach {
                when (visited[it.destination]) {
                    notVisited -> {
                        // it's a candidate
                        destination = it.destination
                    }
                    inPath -> {
                        // Cycle!!
                        backwardEdges.add(it)
                    }
                    completed -> {
                        // dead end
                    }
                }
            }
            if (destination == null) {
                // dead end, pop
                stack.pop().let {
                    visited[it] = completed
                }
            } else {
                // go down this path
                stack.push(destination)
                visited[destination!!] = inPath
            }

            if (stack.isEmpty()) {
                // try to get another graph of forest?
                adjacencyList.entries.firstOrNull { visited[it.key] == notVisited }?.key?.let {
                    stack.push(it)
                    visited[it] = inPath
                }
            }
        }

        return backwardEdges.toList()
    }

    /**
     * Only call that on acyclic graph!
     */
    fun flattenDestination(): Map<GraphNode, Set<GraphNode>> {
        val result = HashMap<GraphNode, Set<GraphNode>>()
        adjacencyList.keys.forEach { vertex ->
            result[vertex] = flattenOf(vertex)
        }
        return result
    }

    private fun flattenOf(node: GraphNode): Set<GraphNode> {
        val result = mutableSetOf<GraphNode>()
        val edgesOf = edgesOf(node)
        result.addAll(edgesOf.map { it.destination })
        edgesOf.forEach {
            result.addAll(flattenOf(it.destination))
        }
        return result
    }

    override fun toString(): String {
        return buildString {
            adjacencyList.forEach { (node, edges) ->
                append("${node.name} : [")
                append(edges.joinToString(" ") { it.destination.name })
                append("]\n")
            }
        }
    }
}
