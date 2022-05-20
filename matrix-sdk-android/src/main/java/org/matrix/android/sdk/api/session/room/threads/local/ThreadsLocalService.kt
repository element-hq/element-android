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

package org.matrix.android.sdk.api.session.room.threads.local

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * This interface defines methods to interact with thread related features.
 * It's the local threads implementation and assumes that the homeserver
 * do not support threads
 */
interface ThreadsLocalService {

    /**
     * Returns a [LiveData] list of all the thread root TimelineEvents that exists at the room level
     */
    fun getAllThreadsLive(): LiveData<List<TimelineEvent>>

    /**
     * Returns a list of all the thread root TimelineEvents that exists at the room level
     */
    fun getAllThreads(): List<TimelineEvent>

    /**
     * Returns a [LiveData] list of all the marked unread threads that exists at the room level
     */
    fun getMarkedThreadNotificationsLive(): LiveData<List<TimelineEvent>>

    /**
     * Returns a list of all the marked unread threads that exists at the room level
     */
    fun getMarkedThreadNotifications(): List<TimelineEvent>

    /**
     * Returns whether or not the current user is participating in the thread
     * @param rootThreadEventId the eventId of the current thread
     */
    fun isUserParticipatingInThread(rootThreadEventId: String): Boolean

    /**
     * Enhance the provided root thread TimelineEvent [List] by adding the latest
     * message edition for that thread
     * @return the enhanced [List] with edited updates
     */
    fun mapEventsWithEdition(threads: List<TimelineEvent>): List<TimelineEvent>

    /**
     * Marks the current thread as read in local DB.
     * note: read receipts within threads are not yet supported with the API
     * @param rootThreadEventId the root eventId of the current thread
     */
    suspend fun markThreadAsRead(rootThreadEventId: String)
}
