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

import im.vector.app.core.di.ScreenScope
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

/*
    This holds an instance of the current room summary.
    You should use this in the context of the timeline.
 */
@ScreenScope
class RoomSummaryHolder @Inject constructor() {

    var roomSummary: RoomSummary? = null
        private set

    fun set(roomSummary: RoomSummary) {
        this.roomSummary = roomSummary
    }

    fun clear() {
        roomSummary = null
    }
}
