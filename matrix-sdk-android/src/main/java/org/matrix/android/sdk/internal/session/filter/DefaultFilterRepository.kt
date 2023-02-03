/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.filter

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.FilterEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal class DefaultFilterRepository @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) : FilterRepository {

    override suspend fun storeSyncFilter(filter: Filter, filterId: String, roomEventFilter: RoomEventFilter) {
        monarchy.awaitTransaction { realm ->
            // We manage only one filter for now
            val filterJson = filter.toJSONString()
            val roomEventFilterJson = roomEventFilter.toJSONString()

            val filterEntity = FilterEntity.getOrCreate(realm)

            filterEntity.filterBodyJson = filterJson
            filterEntity.roomEventFilterJson = roomEventFilterJson
            filterEntity.filterId = filterId
        }
    }

    override suspend fun getStoredSyncFilterBody(): String {
        return monarchy.awaitTransaction {
            FilterEntity.getOrCreate(it).filterBodyJson
        }
    }

    override suspend fun getStoredSyncFilterId(): String? {
        return monarchy.awaitTransaction {
            val id = FilterEntity.get(it)?.filterId
            if (id.isNullOrBlank()) {
                null
            } else {
                id
            }
        }
    }

    override suspend fun getRoomFilterBody(): String {
        return monarchy.awaitTransaction {
            FilterEntity.getOrCreate(it).roomEventFilterJson
        }
    }
}
