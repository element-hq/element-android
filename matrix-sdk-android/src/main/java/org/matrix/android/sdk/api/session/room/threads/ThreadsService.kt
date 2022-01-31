/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.threads

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * This interface defines methods to interact with threads related features. It's implemented at the room level within the main timeline.
 */
interface ThreadsService {

    /**
     * Get a live list of all the TimelineEvents which have thread replies for the specified roomId
     * @return the [LiveData] of [TimelineEvent]
     */
    fun getAllThreadsLive(): LiveData<List<TimelineEvent>>

    /**
     * Get a list of all the TimelineEvents which have thread replies for the specified roomId
     * @return the [LiveData] of [TimelineEvent]
     */
    fun getAllThreads(): List<TimelineEvent>

    /**
     * Get a live list of all the local unread threads for the specified roomId
     * @return the [LiveData] of [TimelineEvent]
     */
    fun getNumberOfLocalThreadNotificationsLive(): LiveData<List<TimelineEvent>>

    /**
     * Get a list of all the local unread threads for the specified roomId
     * @return the [LiveData] of [TimelineEvent]
     */
    fun getNumberOfLocalThreadNotifications(): List<TimelineEvent>

    /**
     * Returns whether or not the current user is participating in the thread
     * @param rootThreadEventId the eventId of the current thread
     */
    fun isUserParticipatingInThread(rootThreadEventId: String): Boolean

    /**
     * Enhance the thread list with the edited events if needed
     * @return the [LiveData] of [TimelineEvent]
     */
    fun mapEventsWithEdition(threads: List<TimelineEvent>): List<TimelineEvent>

    /**
     * Marks the current thread as read. This is a local implementation
     * @param rootThreadEventId the eventId of the current thread
     */
    suspend fun markThreadAsRead(rootThreadEventId: String)
}
