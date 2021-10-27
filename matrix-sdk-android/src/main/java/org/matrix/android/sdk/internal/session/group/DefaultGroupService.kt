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

package org.matrix.android.sdk.internal.session.group

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmQuery
import org.matrix.android.sdk.api.session.group.Group
import org.matrix.android.sdk.api.session.group.GroupService
import org.matrix.android.sdk.api.session.group.GroupSummaryQueryParams
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.GroupEntity
import org.matrix.android.sdk.internal.database.model.GroupSummaryEntity
import org.matrix.android.sdk.internal.database.model.GroupSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.QueryStringValueProcessor
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.util.fetchCopyMap
import javax.inject.Inject

internal class DefaultGroupService @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val groupFactory: GroupFactory,
        private val queryStringValueProcessor: QueryStringValueProcessor,
) : GroupService {

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
        return with(queryStringValueProcessor) {
            GroupSummaryEntity.where(realm)
                    .process(GroupSummaryEntityFields.DISPLAY_NAME, queryParams.displayName)
                    .process(GroupSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
        }
    }
}
