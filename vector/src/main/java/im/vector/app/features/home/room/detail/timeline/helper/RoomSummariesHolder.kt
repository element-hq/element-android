/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.helper

import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject
import javax.inject.Singleton

/*
    You can use this to share room summary instances within the app.
    You should probably use this only in the context of the timeline
 */
@Singleton
class RoomSummariesHolder @Inject constructor() {

    private var roomSummaries = HashMap<String, RoomSummary>()

    fun set(roomSummary: RoomSummary) {
        roomSummaries[roomSummary.roomId] = roomSummary
    }

    fun get(roomId: String) = roomSummaries[roomId]

    fun remove(roomId: String) = roomSummaries.remove(roomId)

    fun clear() {
        roomSummaries.clear()
    }
}
