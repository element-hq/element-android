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

package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

/**
 * Save a filter, in db and if any changes, upload to the server.
 * Return the filterId if uploading to the server is successful, else return null.
 */
internal interface SaveFilterTask : Task<SaveFilterTask.Params, String?> {

    data class Params(
            val filter: Filter
    )
}

internal class DefaultSaveFilterTask @Inject constructor(
        @UserId private val userId: String,
        private val filterAPI: FilterApi,
        private val filterRepository: FilterRepository,
        private val globalErrorReceiver: GlobalErrorReceiver,
) : SaveFilterTask {

    override suspend fun execute(params: SaveFilterTask.Params): String? {
        val filter = params.filter
        val filterResponse = tryOrNull {
            executeRequest(globalErrorReceiver) {
                filterAPI.uploadFilter(userId, filter)
            }
        }

        val filterId = filterResponse?.filterId
        filterRepository.storeSyncFilter(
                filter = filter,
                filterId = filterId.orEmpty(),
                roomEventFilter = FilterFactory.createDefaultRoomFilter()
        )
        return filterId
    }
}
