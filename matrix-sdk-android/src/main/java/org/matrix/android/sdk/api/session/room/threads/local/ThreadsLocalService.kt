/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
     * Returns a [LiveData] list of all the thread root TimelineEvents that exists at the room level.
     */
    fun getAllThreadsLive(): LiveData<List<TimelineEvent>>

    /**
     * Returns a list of all the thread root TimelineEvents that exists at the room level.
     */
    fun getAllThreads(): List<TimelineEvent>

    /**
     * Returns a [LiveData] list of all the marked unread threads that exists at the room level.
     */
    fun getMarkedThreadNotificationsLive(): LiveData<List<TimelineEvent>>

    /**
     * Returns a list of all the marked unread threads that exists at the room level.
     */
    fun getMarkedThreadNotifications(): List<TimelineEvent>

    /**
     * Returns whether or not the current user is participating in the thread.
     * @param rootThreadEventId the eventId of the current thread.
     */
    fun isUserParticipatingInThread(rootThreadEventId: String): Boolean

    /**
     * Enhance the provided root thread TimelineEvent [List] by adding the latest
     * message edition for that thread.
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
