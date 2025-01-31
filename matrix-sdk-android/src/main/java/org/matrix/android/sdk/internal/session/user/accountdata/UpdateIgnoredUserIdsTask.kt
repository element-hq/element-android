/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
    }
}
