/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.threads

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary

/**
 * This interface defines methods to interact with thread related features.
 * It's the dynamic threads implementation and the homeserver must return
 * a capability entry for threads. If the server do not support m.thread
 * then [ThreadsLocalService] should be used instead
 */
interface ThreadsService {

    /**
     * Returns a [LiveData] list of all the [ThreadSummary] that exists at the room level.
     */
    fun getAllThreadSummariesLive(): LiveData<List<ThreadSummary>>

    /**
     * Returns a list of all the [ThreadSummary] that exists at the room level.
     */
    fun getAllThreadSummaries(): List<ThreadSummary>

    /**
     * Enhance the provided ThreadSummary[List] by adding the latest
     * message edition for that thread.
     * @return the enhanced [List] with edited updates
     */
    fun enhanceThreadWithEditions(threads: List<ThreadSummary>): List<ThreadSummary>

    /**
     * Fetch all thread replies for the specified thread using the /relations api.
     * @param rootThreadEventId the root thread eventId
     * @param from defines the token that will fetch from that position
     * @param limit defines the number of max results the api will respond with
     */
    suspend fun fetchThreadTimeline(rootThreadEventId: String, from: String, limit: Int)

    /**
     * Fetch all thread summaries for the current room using the enhanced /messages api.
     */
    suspend fun fetchThreadSummaries()
}
