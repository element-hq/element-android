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

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.FilterEntity
import org.matrix.android.sdk.internal.session.filter.FilterFactory

/**
 * Get the current filter
 */
internal fun FilterEntity.Companion.get(realm: Realm): FilterEntity? {
    return realm.where<FilterEntity>().findFirst()
}

/**
 * Get the current filter, create one if it does not exist
 */
internal fun FilterEntity.Companion.getOrCreate(realm: Realm): FilterEntity {
    return get(realm) ?: realm.createObject<FilterEntity>()
            .apply {
                filterBodyJson = FilterFactory.createDefaultFilter().toJSONString()
                roomEventFilterJson = FilterFactory.createDefaultRoomFilter().toJSONString()
                filterId = ""
            }
}
