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

/**
 * This interface defines methods to interact with the timeline. It's implemented at the room level.
 */
interface TimelineService {

    /**
     * Instantiate a [Timeline] with an optional initial eventId, to be used with permalink.
     * You can filter the type you want to grab with the allowedTypes param.
     * @param eventId the optional initial eventId.
     * @param allowedTypes the optional filter types
     * @return the instantiated timeline
     */
    fun createTimeline(eventId: String?, allowedTypes: List<String>? = null): Timeline


    fun getTimeLineEvent(eventId: String): TimelineEvent?
}