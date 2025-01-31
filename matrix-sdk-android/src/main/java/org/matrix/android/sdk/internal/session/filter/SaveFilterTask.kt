/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.api.session.sync.FilterService
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

/**
 * Save a filter, in db and if any changes, upload to the server.
 */
internal interface SaveFilterTask : Task<SaveFilterTask.Params, Unit> {

    data class Params(
            val filterPreset: FilterService.FilterPreset
    )
}

internal class DefaultSaveFilterTask @Inject constructor(
        @UserId private val userId: String,
        private val filterAPI: FilterApi,
        private val filterRepository: FilterRepository,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SaveFilterTask {

    override suspend fun execute(params: SaveFilterTask.Params) {
        val filterBody = when (params.filterPreset) {
            FilterService.FilterPreset.ElementFilter -> {
                FilterFactory.createElementFilter()
            }
            FilterService.FilterPreset.NoFilter -> {
                FilterFactory.createDefaultFilter()
            }
        }
        val roomFilter = when (params.filterPreset) {
            FilterService.FilterPreset.ElementFilter -> {
                FilterFactory.createElementRoomFilter()
            }
            FilterService.FilterPreset.NoFilter -> {
                FilterFactory.createDefaultRoomFilter()
            }
        }
        val updated = filterRepository.storeFilter(filterBody, roomFilter)
        if (updated) {
            val filterResponse = executeRequest(globalErrorReceiver) {
                // TODO auto retry
                filterAPI.uploadFilter(userId, filterBody)
            }
            filterRepository.storeFilterId(filterBody, filterResponse.filterId)
        }
    }
}
