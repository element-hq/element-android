/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.timeline

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to interact with the timeline. It's implemented at the room level.
 */
interface TimelineService {

    /**
     * Instantiate a [Timeline] with an optional initial eventId, to be used with permalink.
     * You can also configure some settings with the [settings] param.
     *
     * Important: the returned Timeline has to be started
     *
     * @param eventId the optional initial eventId.
     * @param settings settings to configure the timeline.
     * @return the instantiated timeline
     */
    fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline

    /**
     * Returns a snapshot of TimelineEvent event with eventId.
     * At the opposite of getTimelineEventLive which will be updated when local echo event is synced, it will return null in this case.
     * @param eventId the eventId to get the TimelineEvent
     */
    fun getTimelineEvent(eventId: String): TimelineEvent?

    /**
     * Creates a LiveData of Optional TimelineEvent event with eventId.
     * If the eventId is a local echo eventId, it will make the LiveData be updated with the synced TimelineEvent when coming through the sync.
     * In this case, makes sure to use the new synced eventId from the TimelineEvent class if you want to interact, as the local echo is removed from the SDK.
     * @param eventId the eventId to listen for TimelineEvent
     */
    fun getTimelineEventLive(eventId: String): LiveData<Optional<TimelineEvent>>

    /**
     * Returns a snapshot list of TimelineEvent with EventType.MESSAGE and MessageType.MSGTYPE_IMAGE or MessageType.MSGTYPE_VIDEO.
     */
    fun getAttachmentMessages(): List<TimelineEvent>
}
