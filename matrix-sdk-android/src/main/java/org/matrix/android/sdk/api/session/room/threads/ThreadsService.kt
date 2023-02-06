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

import androidx.paging.PagedList
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary

/**
 * This interface defines methods to interact with thread related features.
 * It's the dynamic threads implementation and the homeserver must return
 * a capability entry for threads. If the server do not support m.thread
 * then [org.matrix.android.sdk.api.session.room.threads.local.ThreadsLocalService] should be used instead
 */
interface ThreadsService {

    suspend fun getPagedThreadsList(userParticipating: Boolean, pagedListConfig: PagedList.Config): ThreadLivePageResult

    suspend fun fetchThreadList(nextBatchId: String?, limit: Int, filter: ThreadFilter = ThreadFilter.ALL): FetchThreadsResult

    /**
     * Returns a list of all the [ThreadSummary] that exists at the room level.
     */
    suspend fun getAllThreadSummaries(): List<ThreadSummary>

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
}
