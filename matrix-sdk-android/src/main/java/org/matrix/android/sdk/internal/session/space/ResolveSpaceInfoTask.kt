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
