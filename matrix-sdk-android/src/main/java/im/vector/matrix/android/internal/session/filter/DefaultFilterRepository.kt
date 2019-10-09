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

import im.vector.matrix.android.internal.database.model.FilterEntity
import im.vector.matrix.android.internal.database.model.FilterEntityFields
import im.vector.matrix.android.internal.database.query.getFilter
import im.vector.matrix.android.internal.di.SessionDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import javax.inject.Inject

internal class DefaultFilterRepository @Inject constructor(
        @SessionDatabase private val realmConfiguration: RealmConfiguration
) : FilterRepository {

    override fun storeFilter(filterBody: FilterBody, roomEventFilter: RoomEventFilter): Boolean {
        val result: Boolean

        val realm = Realm.getInstance(realmConfiguration)

        val filter = FilterEntity.getFilter(realm)

        if (filter.filterBodyJson != filterBody.toJSONString()) {
            // Filter has changed, store it and reset the filter Id
            realm.executeTransaction {
                // We manage only one filter for now
                val filterBodyJson = filterBody.toJSONString()
                val roomEventFilterJson = roomEventFilter.toJSONString()

                val filterEntity = FilterEntity.getFilter(it)

                filterEntity.filterBodyJson = filterBodyJson
                filterEntity.roomEventFilterJson = roomEventFilterJson
                // Reset filterId
                filterEntity.filterId = ""
            }
            result = true
        } else {
            result = filter.filterId.isBlank()
        }

        realm.close()

        return result
    }

    override fun storeFilterId(filterBody: FilterBody, filterId: String) {
        val realm = Realm.getInstance(realmConfiguration)

        realm.executeTransaction {
            // We manage only one filter for now
            val filterBodyJson = filterBody.toJSONString()

            // Update the filter id, only if the filter body matches
            it.where<FilterEntity>()
                    .equalTo(FilterEntityFields.FILTER_BODY_JSON, filterBodyJson)
                    ?.findFirst()
                    ?.filterId = filterId
        }

        realm.close()
    }

    override fun getFilter(): String {
        val result: String

        val realm = Realm.getInstance(realmConfiguration)

        val filter = FilterEntity.getFilter(realm)

        result = if (filter.filterId.isBlank()) {
            // Use the Json format
            filter.filterBodyJson
        } else {
            // Use FilterId
            filter.filterId
        }

        realm.close()

        return result
    }

    override fun getRoomFilter(): String {
        val realm = Realm.getInstance(realmConfiguration)

        val filter = FilterEntity.getFilter(realm)

        val result = filter.roomEventFilterJson

        realm.close()

        return result
    }
}
