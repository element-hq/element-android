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

package org.matrix.android.sdk.api.session.group

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.group.model.GroupSummary

/**
 * This interface defines methods to get groups. It's implemented at the session level.
 */
interface GroupService {

    /**
     * Get a group from a groupId
     * @param groupId the groupId to look for.
     * @return the group with groupId or null
     */
    fun getGroup(groupId: String): Group?

    /**
     * Get a groupSummary from a groupId
     * @param groupId the groupId to look for.
     * @return the groupSummary with groupId or null
     */
    fun getGroupSummary(groupId: String): GroupSummary?

    /**
     * Get a list of group summaries. This list is a snapshot of the data.
     * @return the list of [GroupSummary]
     */
    fun getGroupSummaries(groupSummaryQueryParams: GroupSummaryQueryParams): List<GroupSummary>

    /**
     * Get a live list of group summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of [GroupSummary]
     */
    fun getGroupSummariesLive(groupSummaryQueryParams: GroupSummaryQueryParams): LiveData<List<GroupSummary>>
}
