/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.database.mapper

import com.squareup.sqldelight.db.SqlCursor
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.sqldelight.session.Memberships
import javax.inject.Inject

internal class GroupSummaryMapper @Inject constructor() {

    fun map(cursor: SqlCursor): GroupSummary = GroupSummary(
            groupId = cursor.getString(0)!!,
            membership = Membership.valueOf(cursor.getString(4)!!),
            displayName = cursor.getString(1) ?: "",
            shortDescription = cursor.getString(2) ?: "",
            avatarUrl = cursor.getString(3) ?: ""
    )

    fun map(group_id: String,
            display_name: String?,
            short_description: String?,
            avatar_url: String?,
            membership: Memberships): GroupSummary {

        return GroupSummary(
                groupId = group_id,
                membership = membership.map(),
                displayName = display_name ?: "",
                shortDescription = short_description ?: "",
                avatarUrl = avatar_url ?: ""
        )
    }
}
