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
import androidx.paging.PagedList
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateIgnoredUserIdsTask
import org.matrix.android.sdk.internal.session.user.model.SearchUserTask
import javax.inject.Inject

internal class DefaultUserService @Inject constructor(private val userDataSource: UserDataSource,
                                                      private val searchUserTask: SearchUserTask,
                                                      private val updateIgnoredUserIdsTask: UpdateIgnoredUserIdsTask,
                                                      private val getProfileInfoTask: GetProfileInfoTask) : UserService {

    override fun getUser(userId: String): User? {
        return userDataSource.getUser(userId)
    }

    override suspend fun resolveUser(userId: String): User {
        return getUser(userId) ?: run {
            val params = GetProfileInfoTask.Params(userId)
            val json = getProfileInfoTask.execute(params)
            User.fromJson(userId, json)
        }
    }

    override fun getUserLive(userId: String): LiveData<Optional<User>> {
        return userDataSource.getUserLive(userId)
    }

    override fun getUsersLive(): LiveData<List<User>> {
        return userDataSource.getUsersLive()
    }

    override fun getPagedUsersLive(filter: String?, excludedUserIds: Set<String>?): LiveData<PagedList<User>> {
        return userDataSource.getPagedUsersLive(filter, excludedUserIds)
    }

    override fun getIgnoredUsersLive(): LiveData<List<User>> {
        return userDataSource.getIgnoredUsersLive()
    }

    override suspend fun searchUsersDirectory(search: String,
                                              limit: Int,
                                              excludedUserIds: Set<String>): List<User> {
        val params = SearchUserTask.Params(limit, search, excludedUserIds)
        return searchUserTask.execute(params)
    }

    override suspend fun ignoreUserIds(userIds: List<String>) {
        val params = UpdateIgnoredUserIdsTask.Params(userIdsToIgnore = userIds.toList())
        updateIgnoredUserIdsTask.execute(params)
    }

    override suspend fun unIgnoreUserIds(userIds: List<String>) {
        val params = UpdateIgnoredUserIdsTask.Params(userIdsToUnIgnore = userIds.toList())
        updateIgnoredUserIdsTask.execute(params)
    }
}
