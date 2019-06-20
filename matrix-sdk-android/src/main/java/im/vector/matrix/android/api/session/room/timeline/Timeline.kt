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

/**
 * A Timeline instance represents a contiguous sequence of events in a room.
 * <p>
 * There are two kinds of timeline:
 * <p>
 * - live timelines: they process live events from the sync. You can paginate
 * backwards but not forwards.
 * <p>
 * - past timelines: they start in the past from an `initialEventId`. You can paginate
 * backwards and forwards.
 *
 */
interface Timeline {

    var listener: Listener?

    /**
     * This should be called before any other method after creating the timeline. It ensures the underlying database is open
     */
    fun start()

    /**
     * This should be called when you don't need the timeline. It ensures the underlying database get closed.
     */
    fun dispose()

    /**
     * Check if the timeline can be enriched by paginating.
     * @param the direction to check in
     * @return true if timeline can be enriched
     */
    fun hasMoreToLoad(direction: Direction): Boolean

    /**
     * This is the main method to enrich the timeline with new data.
     * It will call the onUpdated method from [Listener] when the data will be processed.
     * It also ensures only one pagination by direction is launched at a time, so you can safely call this multiple time in a row.
     */
    fun paginate(direction: Direction, count: Int)


    interface Listener {
        /**
         * Call when the timeline has been updated through pagination or sync.
         * @param snapshot the most uptodate snapshot
         */
        fun onUpdated(snapshot: List<TimelineEvent>)
    }

    /**
     * This is used to paginate in one or another direction.
     */
    enum class Direction {
        /**
         * It represents future events.
         */
        FORWARDS,
        /**
         * It represents past events.
         */
        BACKWARDS
    }

}