/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store.db.query

import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmSingleQuery
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.database.queryIn

internal class UserEntityQueries(realm: TypedRealm) : TypedRealm by realm {

    fun all(): RealmQuery<UserEntity> {
        return query(UserEntity::class)
    }

    fun byUserId(userId: String): RealmQuery<UserEntity> {
        return all().query("userId == $0", userId)
    }

    fun firstUserId(userId: String): RealmSingleQuery<UserEntity> {
        return byUserId(userId).first()
    }

    fun byUserIds(userIds: List<String>): RealmQuery<UserEntity> {
        return all().queryIn("userId", userIds)
    }
}

internal fun TypedRealm.userEntityQueries() = UserEntityQueries(this)
