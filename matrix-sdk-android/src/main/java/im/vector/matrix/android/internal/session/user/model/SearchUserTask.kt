/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.user.model

import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.user.SearchUserAPI
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface SearchUserTask : Task<SearchUserTask.Params, List<User>> {

    data class Params(
            val limit: Int,
            val search: String,
            val excludedUserIds: Set<String>
    )
}

internal class DefaultSearchUserTask @Inject constructor(private val searchUserAPI: SearchUserAPI) : SearchUserTask {

    override suspend fun execute(params: SearchUserTask.Params): List<User> {
        val response = executeRequest<SearchUsersRequestResponse> {
            apiCall = searchUserAPI.searchUsers(SearchUsersParams(params.search, params.limit))
        }
        return response.users.map {
            User(it.userId, it.displayName, it.avatarUrl)
        }
    }

}