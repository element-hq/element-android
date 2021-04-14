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

package org.matrix.android.sdk.internal.session.user.accountdata

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.internal.database.model.IgnoredUserEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.sync.model.accountdata.IgnoredUsersContent
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateIgnoredUserIdsTask : Task<UpdateIgnoredUserIdsTask.Params, Unit> {

    data class Params(
            val userIdsToIgnore: List<String> = emptyList(),
            val userIdsToUnIgnore: List<String> = emptyList()
    )
}

internal class DefaultUpdateIgnoredUserIdsTask @Inject constructor(
        private val accountDataApi: AccountDataAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val saveIgnoredUsersTask: SaveIgnoredUsersTask,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdateIgnoredUserIdsTask {

    override suspend fun execute(params: UpdateIgnoredUserIdsTask.Params) {
        // Get current list
        val ignoredUserIds = monarchy.fetchAllMappedSync(
                { realm -> realm.where(IgnoredUserEntity::class.java) },
                { it.userId }
        ).toMutableSet()

        val original = ignoredUserIds.toSet()

        ignoredUserIds.removeAll { it in params.userIdsToUnIgnore }
        ignoredUserIds.addAll(params.userIdsToIgnore)

        if (original == ignoredUserIds) {
            // No change
            return
        }

        val list = ignoredUserIds.toList()
        val body = IgnoredUsersContent.createWithUserIds(list)

        executeRequest(globalErrorReceiver) {
            accountDataApi.setAccountData(userId, UserAccountDataTypes.TYPE_IGNORED_USER_LIST, body)
        }

        // Update the DB right now (do not wait for the sync to come back with updated data, for a faster UI update)
        saveIgnoredUsersTask.execute(SaveIgnoredUsersTask.Params(list))
    }
}
