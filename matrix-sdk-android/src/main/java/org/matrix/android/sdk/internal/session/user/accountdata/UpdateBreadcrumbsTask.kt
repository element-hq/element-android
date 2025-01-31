/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.accountdata

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.BreadcrumbsEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.sync.model.accountdata.BreadcrumbsContent
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.fetchCopied
import javax.inject.Inject

// Use the same arbitrary value than Riot-Web
private const val MAX_BREADCRUMBS_ROOMS_NUMBER = 20

internal interface UpdateBreadcrumbsTask : Task<UpdateBreadcrumbsTask.Params, Unit> {
    data class Params(
            val newTopRoomId: String
    )
}

internal class DefaultUpdateBreadcrumbsTask @Inject constructor(
        private val saveBreadcrumbsTask: SaveBreadcrumbsTask,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        @SessionDatabase private val monarchy: Monarchy
) : UpdateBreadcrumbsTask {

    override suspend fun execute(params: UpdateBreadcrumbsTask.Params) {
        val newBreadcrumbs =
                // Get the breadcrumbs entity, if any
                monarchy.fetchCopied { BreadcrumbsEntity.get(it) }
                        ?.recentRoomIds
                        ?.apply {
                            // Modify the list to add the newTopRoomId first
                            // Ensure the newTopRoomId is not already in the list
                            remove(params.newTopRoomId)
                            // Add the newTopRoomId at first position
                            add(0, params.newTopRoomId)
                        }
                        ?.take(MAX_BREADCRUMBS_ROOMS_NUMBER)
                        ?: listOf(params.newTopRoomId)

        // Update the DB locally, do not wait for the sync
        saveBreadcrumbsTask.execute(SaveBreadcrumbsTask.Params(newBreadcrumbs))

        // FIXME It can remove the previous breadcrumbs, if not synced yet
        // And update account data
        updateUserAccountDataTask.execute(
                UpdateUserAccountDataTask.BreadcrumbsParams(
                        breadcrumbsContent = BreadcrumbsContent(newBreadcrumbs)
                )
        )
    }
}
