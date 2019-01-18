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

package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.model.GroupEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun GroupEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<GroupEntity> {
    return realm.where<GroupEntity>().equalTo(GroupEntityFields.GROUP_ID, roomId)
}

internal fun GroupEntity.Companion.where(realm: Realm, membership: MyMembership? = null): RealmQuery<GroupEntity> {
    val query = realm.where<GroupEntity>()
    if (membership != null) {
        query.equalTo(GroupEntityFields.MEMBERSHIP_STR, membership.name)
    }
    return query
}
