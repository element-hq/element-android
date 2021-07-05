/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.android.sdk.internal.session.users

import fr.gouv.tchap.android.sdk.api.session.userinfo.model.UserInfo
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetUsersInfoTask : Task<GetUsersInfoParams, Map<String, UserInfo>>

internal class TchapGetUsersInfoTask @Inject constructor(
        private val usersAPI: UsersInfoAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetUsersInfoTask {

    override suspend fun execute(params: GetUsersInfoParams): Map<String, UserInfo> {
        val result = executeRequest(globalErrorReceiver) {
            usersAPI.getUsersInfo(params)
        }

        return result.mapValues {
            UserInfo(
                    expired = it.value.expired,
                    deactivated = it.value.deactivated
            )
        }
    }
}
