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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.IgnoredUserEntity
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountData
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataIgnoredUsers
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface UpdateIgnoredUserIdsTask : Task<UpdateIgnoredUserIdsTask.Params, Unit> {

    data class Params(
            val userIdsToIgnore: List<String> = emptyList(),
            val userIdsToUnIgnore: List<String> = emptyList()
    )

}

internal class DefaultUpdateIgnoredUserIdsTask @Inject constructor(private val accountDataApi: AccountDataAPI,
                                                                   private val monarchy: Monarchy,
                                                                   @UserId private val userId: String) : UpdateIgnoredUserIdsTask {

    override suspend fun execute(params: UpdateIgnoredUserIdsTask.Params) {
        // Get current list
        val ignoredUserIds = monarchy.fetchAllMappedSync(
                { realm -> realm.where(IgnoredUserEntity::class.java) },
                { it.userId }
        )

        val original = ignoredUserIds.toList()

        ignoredUserIds -= params.userIdsToUnIgnore
        ignoredUserIds += params.userIdsToIgnore

        if (original == ignoredUserIds) {
            // No change
            return
        }

        val body = UserAccountDataIgnoredUsers.createWithUserIds(ignoredUserIds)

        return executeRequest {
            apiCall = accountDataApi.setAccountData(userId, UserAccountData.TYPE_IGNORED_USER_LIST, body)
        }
    }
}
