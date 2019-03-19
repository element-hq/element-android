/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.api.session.room.timeline

interface Timeline {

    var listener: Timeline.Listener?

    fun hasMoreToLoad(direction: Direction): Boolean
    fun hasReachedEnd(direction: Direction): Boolean
    fun size(): Int
    fun snapshot(): List<TimelineEvent>
    fun paginate(direction: Direction, count: Int)
    fun start()
    fun dispose()

    interface Listener {
        fun onUpdated(snapshot: List<TimelineEvent>)
    }

    enum class Direction(val value: String) {
        /**
         * Forwards when the event is added to the end of the timeline.
         * These events come from the /sync stream or from forwards pagination.
         */
        FORWARDS("f"),

        /**
         * Backwards when the event is added to the start of the timeline.
         * These events come from a back pagination.
         */
        BACKWARDS("b");
    }


}