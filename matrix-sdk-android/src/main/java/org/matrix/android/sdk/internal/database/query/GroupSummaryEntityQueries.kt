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
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.GroupSummaryEntity
import org.matrix.android.sdk.internal.database.model.GroupSummaryEntityFields

internal fun GroupSummaryEntity.Companion.where(realm: Realm, groupId: String? = null): RealmQuery<GroupSummaryEntity> {
    val query = realm.where<GroupSummaryEntity>()
    if (groupId != null) {
        query.equalTo(GroupSummaryEntityFields.GROUP_ID, groupId)
    }
    return query
}

internal fun GroupSummaryEntity.Companion.where(realm: Realm, groupIds: List<String>): RealmQuery<GroupSummaryEntity> {
    return realm.where<GroupSummaryEntity>()
            .`in`(GroupSummaryEntityFields.GROUP_ID, groupIds.toTypedArray())
}

internal fun GroupSummaryEntity.Companion.getOrCreate(realm: Realm, groupId: String): GroupSummaryEntity {
    return where(realm, groupId).findFirst() ?: realm.createObject(groupId)
}
