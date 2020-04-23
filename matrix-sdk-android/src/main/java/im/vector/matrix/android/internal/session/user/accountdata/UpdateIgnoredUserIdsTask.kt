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

package im.vector.matrix.android.internal.session.user.accountdata

import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.sync.model.accountdata.IgnoredUsersContent
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountData
import im.vector.matrix.android.internal.session.user.UserDataSource
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface UpdateIgnoredUserIdsTask : Task<UpdateIgnoredUserIdsTask.Params, Unit> {

    data class Params(
            val userIdsToIgnore: List<String> = emptyList(),
            val userIdsToUnIgnore: List<String> = emptyList()
    )
}

internal class DefaultUpdateIgnoredUserIdsTask @Inject constructor(
        private val accountDataApi: AccountDataAPI,
        private val userDataSource: UserDataSource,
        private val saveIgnoredUsersTask: SaveIgnoredUsersTask,
        @UserId private val userId: String,
        private val eventBus: EventBus
) : UpdateIgnoredUserIdsTask {

    override suspend fun execute(params: UpdateIgnoredUserIdsTask.Params) {
        // Get current list
        val ignoredUserIds = userDataSource.getAllIgnoredIds().toMutableSet()
        val original = ignoredUserIds.toSet()

        ignoredUserIds.removeAll { it in params.userIdsToUnIgnore }
        ignoredUserIds.addAll(params.userIdsToIgnore)

        if (original == ignoredUserIds) {
            // No change
            return
        }

        val list = ignoredUserIds.toList()
        val body = IgnoredUsersContent.createWithUserIds(list)

        executeRequest<Unit>(eventBus) {
            apiCall = accountDataApi.setAccountData(userId, UserAccountData.TYPE_IGNORED_USER_LIST, body)
        }

        // Update the DB right now (do not wait for the sync to come back with updated data, for a faster UI update)
        saveIgnoredUsersTask.execute(SaveIgnoredUsersTask.Params(list))
    }
}
