/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.filter

import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultFilterRepository @Inject constructor(private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                           private val sessionDatabase: SessionDatabase) : FilterRepository {

    override suspend fun storeFilter(filterBody: FilterBody, roomEventFilter: RoomEventFilter): Boolean = withContext(coroutineDispatchers.dbTransaction) {
        val filter = sessionDatabase.filterQueries.get().executeAsOneOrNull()
        val hasChanged = filter == null
                         || filter.filter_body_json != filterBody.toJSONString()
                         || filter.filter_id.isNullOrBlank()
        if (hasChanged) {
            sessionDatabase.filterQueries.insertFilters(filterBody.toJSONString(), roomEventFilter.toJSONString())
        }
        hasChanged
    }

    override suspend fun storeFilterId(filterBody: FilterBody, filterId: String) = withContext(coroutineDispatchers.dbTransaction) {
        sessionDatabase.filterQueries.updateFilterId(filterId, filterBody.toJSONString())
    }

    override suspend fun getFilter(): String = withContext(coroutineDispatchers.dbQuery) {
        val filter = sessionDatabase.filterQueries.getFilterBodyOrId().executeAsOneOrNull()?.expr
        if (filter == null) {
            val filterBody = FilterFactory.createDefaultFilterBody().toJSONString()
            val roomFilter = FilterFactory.createDefaultRoomFilter().toJSONString()
            sessionDatabase.filterQueries.insertFilters(filterBody, roomFilter)
            filterBody
        } else {
            filter
        }
    }

    override suspend fun getRoomFilter(): String = withContext(coroutineDispatchers.dbQuery) {
        val filter = sessionDatabase.filterQueries.getRoomFilterJson().executeAsOneOrNull()
        if (filter == null) {
            val filterBody = FilterFactory.createDefaultFilterBody().toJSONString()
            val roomFilter = FilterFactory.createDefaultRoomFilter().toJSONString()
            sessionDatabase.filterQueries.insertFilters(filterBody, roomFilter)
            roomFilter
        } else {
            filter
        }
    }
}
