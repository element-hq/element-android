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

import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity


internal object GroupSummaryMapper {

    fun map(groupSummaryEntity: GroupSummaryEntity): GroupSummary {
        return GroupSummary(
                groupSummaryEntity.groupId,
                groupSummaryEntity.membership,
                groupSummaryEntity.displayName,
                groupSummaryEntity.shortDescription,
                groupSummaryEntity.avatarUrl,
                groupSummaryEntity.roomIds.toList(),
                groupSummaryEntity.userIds.toList()
        )
    }
}

internal fun GroupSummaryEntity.asDomain(): GroupSummary {
    return GroupSummaryMapper.map(this)
}