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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.FilterEntity
import im.vector.matrix.android.internal.database.model.FilterEntityFields
import im.vector.matrix.android.internal.database.query.getFilter
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import io.realm.kotlin.where
import javax.inject.Inject

internal class DefaultFilterRepository @Inject constructor(private val monarchy: Monarchy) : FilterRepository {

    override suspend fun storeFilter(filterBody: FilterBody, roomEventFilter: RoomEventFilter): Boolean {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            val filter = FilterEntity.getFilter(realm)
            val result = if (filter.filterBodyJson != filterBody.toJSONString()) {
                // Filter has changed, store it and reset the filter Id
                monarchy.awaitTransaction {
                    // We manage only one filter for now
                    val filterBodyJson = filterBody.toJSONString()
                    val roomEventFilterJson = roomEventFilter.toJSONString()

                    val filterEntity = FilterEntity.getFilter(it)

                    filterEntity.filterBodyJson = filterBodyJson
                    filterEntity.roomEventFilterJson = roomEventFilterJson
                    // Reset filterId
                    filterEntity.filterId = ""
                }
                true
            } else {
                filter.filterId.isBlank()
            }
            result
        }
    }

    override suspend fun storeFilterId(filterBody: FilterBody, filterId: String) {
        monarchy.awaitTransaction {
            // We manage only one filter for now
            val filterBodyJson = filterBody.toJSONString()

            // Update the filter id, only if the filter body matches
            it.where<FilterEntity>()
                    .equalTo(FilterEntityFields.FILTER_BODY_JSON, filterBodyJson)
                    ?.findFirst()
                    ?.filterId = filterId
        }
    }

    override suspend fun getFilter(): String {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            val filter = FilterEntity.getFilter(it)
            if (filter.filterId.isBlank()) {
                // Use the Json format
                filter.filterBodyJson
            } else {
                // Use FilterId
                filter.filterId
            }
        }
    }

    override suspend fun getRoomFilter(): String {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            FilterEntity.getFilter(it).roomEventFilterJson
        }
    }
}
