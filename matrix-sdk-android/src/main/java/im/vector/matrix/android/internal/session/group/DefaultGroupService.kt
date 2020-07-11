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

package im.vector.matrix.android.internal.session.group

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.group.GroupSummaryQueryParams
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.query.process
import im.vector.matrix.android.internal.util.fetchCopyMap
import io.realm.Realm
import io.realm.RealmQuery
import javax.inject.Inject

internal class DefaultGroupService @Inject constructor(@SessionDatabase private val monarchy: Monarchy,
                                                       private val groupFactory: GroupFactory) : GroupService {

    override fun getGroup(groupId: String): Group? {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            GroupEntity.where(realm, groupId).findFirst()?.let {
                groupFactory.create(groupId)
            }
        }
    }

    override fun getGroupSummary(groupId: String): GroupSummary? {
        return monarchy.fetchCopyMap(
                { realm -> GroupSummaryEntity.where(realm, groupId).findFirst() },
                { it, _ -> it.asDomain() }
        )
    }

    override fun getGroupSummaries(groupSummaryQueryParams: GroupSummaryQueryParams): List<GroupSummary> {
        return monarchy.fetchAllMappedSync(
                { groupSummariesQuery(it, groupSummaryQueryParams) },
                { it.asDomain() }
        )
    }

    override fun getGroupSummariesLive(groupSummaryQueryParams: GroupSummaryQueryParams): LiveData<List<GroupSummary>> {
        return monarchy.findAllMappedWithChanges(
                { groupSummariesQuery(it, groupSummaryQueryParams) },
                { it.asDomain() }
        )
    }

    private fun groupSummariesQuery(realm: Realm, queryParams: GroupSummaryQueryParams): RealmQuery<GroupSummaryEntity> {
        return GroupSummaryEntity.where(realm)
                .process(GroupSummaryEntityFields.DISPLAY_NAME, queryParams.displayName)
                .process(GroupSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
    }
}
