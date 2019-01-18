/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.api.session.room.timeline

import androidx.paging.PagedList

/**
 * This data class is a holder for timeline data.
 * It's returned by [TimelineService]
 */
data class TimelineData(

        /**
         * The [PagedList] of [TimelineEvent] to usually be render in a RecyclerView.
         */
        val events: PagedList<TimelineEvent>,

        /**
         * True if Timeline is currently paginating forward on server
         */
        val isLoadingForward: Boolean = false,

        /**
         * True if Timeline is currently paginating backward on server
         */
        val isLoadingBackward: Boolean = false
)
