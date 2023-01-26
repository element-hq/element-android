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

/**
 * Represent the status of the loaded polls for a room.
 */
data class LoadedPollsStatus(
        /**
         * Indicate whether more polls can be loaded from timeline.
         * A false value would mean the start of the timeline has been reached.
         */
        val canLoadMore: Boolean,

        /**
         * Number of days of timeline events currently synced (fetched and stored in local).
         */
        val daysSynced: Int,

        /**
         * Indicate whether a sync of timeline events has been completely done in backward. It would
         * mean timeline events have been synced for at least a number of days defined by [PollHistoryService.loadingPeriodInDays].
         */
        val hasCompletedASyncBackward: Boolean,
)
