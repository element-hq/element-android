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

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.room.model.Membership

fun groupSummaryQueryParams(init: (GroupSummaryQueryParams.Builder.() -> Unit) = {}): GroupSummaryQueryParams {
    return GroupSummaryQueryParams.Builder().apply(init).build()
}

/**
 * This class can be used to filter group summaries
 */
data class GroupSummaryQueryParams(
        val displayName: QueryStringValue,
        val memberships: List<Membership>
) {

    class Builder {

        var displayName: QueryStringValue = QueryStringValue.IsNotEmpty
        var memberships: List<Membership> = Membership.all()

        fun build() = GroupSummaryQueryParams(
                displayName = displayName,
                memberships = memberships
        )
    }
}
