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
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateIgnoredUserIdsTask
import org.matrix.android.sdk.internal.session.user.model.SearchUserTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import javax.inject.Inject

internal class DefaultUserService @Inject constructor(private val userDataSource: UserDataSource,
                                                      private val searchUserTask: SearchUserTask,
                                                      private val updateIgnoredUserIdsTask: UpdateIgnoredUserIdsTask,
                                                      private val getProfileInfoTask: GetProfileInfoTask,
                                                      private val taskExecutor: TaskExecutor) : UserService {

    override fun getUser(userId: String): User? {
        return userDataSource.getUser(userId)
    }

    override fun resolveUser(userId: String, callback: MatrixCallback<User>) {
        val known = getUser(userId)
        if (known != null) {
            callback.onSuccess(known)
        } else {
            val params = GetProfileInfoTask.Params(userId)
            getProfileInfoTask
                    .configureWith(params) {
                        this.callback = object : MatrixCallback<JsonDict> {
                            override fun onSuccess(data: JsonDict) {
                                callback.onSuccess(
                                        User(
                                                userId,
                                                data[ProfileService.DISPLAY_NAME_KEY] as? String,
                                                data[ProfileService.AVATAR_URL_KEY] as? String)
                                )
                            }

                            override fun onFailure(failure: Throwable) {
                                callback.onFailure(failure)
                            }
                        }
                    }
                    .executeBy(taskExecutor)
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

    override fun searchUsersDirectory(search: String,
                                      limit: Int,
                                      excludedUserIds: Set<String>,
                                      callback: MatrixCallback<List<User>>): Cancelable {
        val params = SearchUserTask.Params(limit, search, excludedUserIds)
        return searchUserTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun ignoreUserIds(userIds: List<String>, callback: MatrixCallback<Unit>): Cancelable {
        val params = UpdateIgnoredUserIdsTask.Params(userIdsToIgnore = userIds.toList())
        return updateIgnoredUserIdsTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun unIgnoreUserIds(userIds: List<String>, callback: MatrixCallback<Unit>): Cancelable {
        val params = UpdateIgnoredUserIdsTask.Params(userIdsToUnIgnore = userIds.toList())
        return updateIgnoredUserIdsTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
