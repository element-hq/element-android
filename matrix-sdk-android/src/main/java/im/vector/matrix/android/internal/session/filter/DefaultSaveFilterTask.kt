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

package im.vector.matrix.android.internal.session.filter

import arrow.core.Try
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject


/**
 * Save a filter to the server
 */
internal interface SaveFilterTask : Task<SaveFilterTask.Params, Unit> {

    data class Params(
            val filter: FilterBody
    )

}

internal class DefaultSaveFilterTask @Inject constructor(private val sessionParams: SessionParams,
                                                         private val filterAPI: FilterApi,
                                                         private val filterRepository: FilterRepository
) : SaveFilterTask {

    override suspend fun execute(params: SaveFilterTask.Params): Try<Unit> {
        return executeRequest<FilterResponse> {
            // TODO auto retry
            apiCall = filterAPI.uploadFilter(sessionParams.credentials.userId, params.filter)
        }.flatMap { filterResponse ->
            Try {
                filterRepository.storeFilterId(params.filter, filterResponse.filterId)
            }
        }
    }

}