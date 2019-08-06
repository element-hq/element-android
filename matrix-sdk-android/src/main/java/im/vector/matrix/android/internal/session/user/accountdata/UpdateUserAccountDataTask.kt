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

package im.vector.matrix.android.internal.session.user.accountdata

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.sync.model.UserAccountData
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface UpdateUserAccountDataTask : Task<UpdateUserAccountDataTask.Params, Unit> {

    interface Params {
        val type: String
        fun getData(): Any
    }

    data class DirectChatParams(override val type: String = UserAccountData.TYPE_DIRECT_MESSAGES,
                                private val directMessages: Map<String, List<String>>
    ) : Params {

        override fun getData(): Any {
            return directMessages
        }
    }


}

internal class DefaultUpdateUserAccountDataTask @Inject constructor(private val accountDataApi: AccountDataAPI,
                                                                    private val credentials: Credentials) : UpdateUserAccountDataTask {

    override suspend fun execute(params: UpdateUserAccountDataTask.Params) {
        return executeRequest {
            apiCall = accountDataApi.setAccountData(credentials.userId, params.type, params.getData())
        }
    }

}