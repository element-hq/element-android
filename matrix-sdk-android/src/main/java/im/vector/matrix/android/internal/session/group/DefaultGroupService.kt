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

package im.vector.matrix.android.internal.session.group

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where

internal class DefaultGroupService(private val monarchy: Monarchy) : GroupService {

    override fun getGroup(groupId: String): Group? {
        return null
    }

    override fun liveGroupSummaries(): LiveData<List<GroupSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> GroupSummaryEntity.where(realm).isNotEmpty(GroupSummaryEntityFields.DISPLAY_NAME) },
                { it.asDomain() }
        )
    }

}