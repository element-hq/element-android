/*
 * Copyright 2019 New Vector Ltd
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

    fun getTimeLineEvent(eventId: String): TimelineEvent?

    fun getTimeLineEventLive(eventId: String): LiveData<Optional<TimelineEvent>>

    fun getAttachmentMessages() : List<TimelineEvent>
}
