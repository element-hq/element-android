/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.room.timeline

import androidx.lifecycle.LiveData

/**
 * This interface defines methods to interact with the timeline. It's implemented at the room level.
 */
interface TimelineService {

    /**
     * This is the main method of the service. It allows to listen for live [TimelineData].
     * It's automatically refreshed as soon as timeline data gets updated, through sync or pagination.
     *
     * @param eventId: an optional eventId to start loading timeline around.
     * @return the [LiveData] of [TimelineData]
     */
    fun timeline(eventId: String? = null): LiveData<TimelineData>

}