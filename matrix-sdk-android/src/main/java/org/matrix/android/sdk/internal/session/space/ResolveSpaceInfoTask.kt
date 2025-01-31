/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.space

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import retrofit2.HttpException
import javax.inject.Inject

internal interface ResolveSpaceInfoTask : Task<ResolveSpaceInfoTask.Params, SpacesResponse> {
    data class Params(
            val spaceId: String,
            val limit: Int?,
            val maxDepth: Int?,
            val from: String?,
            val suggestedOnly: Boolean?
    )
}

internal class DefaultResolveSpaceInfoTask @Inject constructor(
        private val spaceApi: SpaceApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : ResolveSpaceInfoTask {

    override suspend fun execute(params: ResolveSpaceInfoTask.Params) = executeRequest(globalErrorReceiver) {
        try {
            getSpaceHierarchy(params)
        } catch (e: HttpException) {
            getUnstableSpaceHierarchy(params)
        }
    }

    private suspend fun getSpaceHierarchy(params: ResolveSpaceInfoTask.Params) =
            spaceApi.getSpaceHierarchy(
                    spaceId = params.spaceId,
                    suggestedOnly = params.suggestedOnly,
                    limit = params.limit,
                    maxDepth = params.maxDepth,
                    from = params.from,
            )

    private suspend fun getUnstableSpaceHierarchy(params: ResolveSpaceInfoTask.Params) =
            spaceApi.getSpaceHierarchyUnstable(
                    spaceId = params.spaceId,
                    suggestedOnly = params.suggestedOnly,
                    limit = params.limit,
                    maxDepth = params.maxDepth,
                    from = params.from,
            )
}
