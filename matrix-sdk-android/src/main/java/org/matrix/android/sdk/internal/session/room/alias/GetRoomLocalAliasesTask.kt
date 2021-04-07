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

package org.matrix.android.sdk.internal.session.room.alias

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetRoomLocalAliasesTask : Task<GetRoomLocalAliasesTask.Params, List<String>> {
    data class Params(
            val roomId: String
    )
}

internal class DefaultGetRoomLocalAliasesTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetRoomLocalAliasesTask {

    override suspend fun execute(params: GetRoomLocalAliasesTask.Params): List<String> {
        // We do not check for "org.matrix.msc2432", so the API may be missing
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.getAliases(roomId = params.roomId)
        }

        return response.aliases
    }
}
