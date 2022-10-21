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

package org.matrix.android.sdk.internal.session.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.paging.PagedList
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.IgnoredUserEntity
import org.matrix.android.sdk.internal.database.model.UserEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.queryNotIn
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class UserDataSource @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
) {

    private fun mapUser(userEntity: UserEntity): User {
        return userEntity.asDomain()
    }

    private val pagedListConfig by lazy {
        PagedList.Config.Builder().setPageSize(100).setEnablePlaceholders(false).build()
    }

    fun getUser(userId: String): User? {
        val realm = realmInstance.getBlockingRealm()
        return UserEntity.where(realm, userId)
                .first()
                .find()
                ?.asDomain()
    }

    fun getUserOrDefault(userId: String): User = getUser(userId) ?: User(userId)

    fun getUserLive(userId: String): LiveData<Optional<User>> {
        return realmInstance.queryFirstMapped(this::mapUser) {
            UserEntity.where(it, userId).first()
        }.asLiveData()
    }

    fun getUsersLive(): LiveData<List<User>> {
        return realmInstance.queryList(this::mapUser) { realm ->
            realm.query(UserEntity::class)
                    .query("userId != ''")
                    .sort("displayName")
        }.asLiveData()
    }

    fun getPagedUsersLive(filter: String?, excludedUserIds: Set<String>?): LiveData<PagedList<User>> {
        return realmInstance.queryPagedList(pagedListConfig, this::mapUser) { realm ->
            var query = realm.query(UserEntity::class)
            query = if (filter.isNullOrEmpty()) {
                query.query("userId != ''")
            } else {
                query.query("displayName CONTAINS[c] $0 OR userId CONTAINS $0", filter)
            }
            excludedUserIds
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        query = query.queryNotIn("userId", it)
                    }
            query.sort("displayName")
        }.asLiveData()
    }

    fun getIgnoredUsersLive(): LiveData<List<User>> {
        fun mapper(ignoredUserEntity: IgnoredUserEntity): User {
            return getUser(ignoredUserEntity.userId) ?: User(userId = ignoredUserEntity.userId)
        }

        return realmInstance.queryList(::mapper) { realm ->
            realm.query(IgnoredUserEntity::class)
                    .query("userId != ''")
                    .sort("userId")
        }.asLiveData()
    }
}
