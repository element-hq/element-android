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
package im.vector.matrix.android.internal.session.pushers

import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.mapper.PushersMapper
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetPushersTask : Task<Unit, Unit>

internal class DefaultGetPushersTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        private val sessionDatabase: SessionDatabase,
        private val pushersMapper: PushersMapper,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val eventBus: EventBus
) : GetPushersTask {

    override suspend fun execute(params: Unit) {
        val response = executeRequest<GetPushersResponse>(eventBus) {
            apiCall = pushersAPI.getPushers()
        }
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            // clear existings?
            sessionDatabase.pushersQueries.deleteAll()
            response.pushers?.forEach { jsonPusher ->
                val pusherEntity = pushersMapper.map(jsonPusher, PusherState.REGISTERED)
                sessionDatabase.pushersQueries.insertOrReplace(pusherEntity)
            }
        }
    }
}
