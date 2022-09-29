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

import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.model.FilterEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class DefaultFilterRepository @Inject constructor(@SessionDatabase private val realmInstance: RealmInstance) : FilterRepository {

    override suspend fun storeFilter(filter: Filter, roomEventFilter: RoomEventFilter): Boolean {
        val realm = realmInstance.getRealm()
        val filterEntity = FilterEntity.get(realm)
        // Filter has changed, or no filter Id yet
        val hasChanged = filterEntity == null ||
                filterEntity.filterBodyJson != filter.toJSONString() ||
                filterEntity.filterId.isBlank()
        if (hasChanged) {
            // Filter is new or has changed, store it and reset the filter Id.
            realmInstance.write {
                // We manage only one filter for now
                val filterJson = filter.toJSONString()
                val roomEventFilterJson = roomEventFilter.toJSONString()
                FilterEntity.getOrCreate(this).apply {
                    this.filterBodyJson = filterJson
                    this.roomEventFilterJson = roomEventFilterJson
                    // Reset filterId
                    this.filterId = ""
                }
            }
        }
        return hasChanged
    }

        override suspend fun storeFilterId(filter: Filter, filterId: String) {
            realmInstance.write {
                // We manage only one filter for now
                val filterJson = filter.toJSONString()

                // Update the filter id, only if the filter body matches
                query(FilterEntity::class)
                        .query("filterBodyJson == $0", filterJson)
                        .first()
                        .find()
                        ?.filterId = filterId
            }
        }

        override suspend fun getFilter(): String {
            return realmInstance.write {
                val filter = FilterEntity.getOrCreate(this)
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
            return realmInstance.write {
                FilterEntity.getOrCreate(this).roomEventFilterJson
            }
        }
    }
