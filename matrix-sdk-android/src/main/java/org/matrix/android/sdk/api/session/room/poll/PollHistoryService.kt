/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.poll

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.model.PollResponseAggregatedSummary

/**
 * Expose methods to get history of polls in rooms.
 */
interface PollHistoryService {

    val loadingPeriodInDays: Int

    /**
     * Ask to load more polls starting from last loaded polls for a period defined by
     * [loadingPeriodInDays].
     */
    suspend fun loadMore(): LoadedPollsStatus

    /**
     * Get the current status of the loaded polls.
     */
    fun getLoadedPollsStatus(): LoadedPollsStatus

    /**
     * Sync polls from last loaded polls until now.
     */
    suspend fun syncPolls()

    /**
     * Get currently loaded list of polls. See [loadMore].
     */
    fun getPolls(): LiveData<List<PollResponseAggregatedSummary>>
}
