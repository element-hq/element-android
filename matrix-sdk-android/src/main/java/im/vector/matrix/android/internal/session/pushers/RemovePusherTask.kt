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
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface RemovePusherTask : Task<RemovePusherTask.Params, Unit> {
    data class Params(val pushKey: String,
                      val pushAppId: String)
}

internal class DefaultRemovePusherTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        private val sessionDatabase: SessionDatabase,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val eventBus: EventBus
) : RemovePusherTask {

    override suspend fun execute(params: RemovePusherTask.Params) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            sessionDatabase.pushersQueries.updateState(PusherState.UNREGISTERING.name, params.pushKey)
        }
        val existing = sessionDatabase.pushersQueries.getWithPushKey(params.pushKey).executeAsOneOrNull()
                ?: throw Exception("No existing pusher")

        val deleteBody = JsonPusher(
                pushKey = params.pushKey,
                appId = params.pushAppId,
                // kind null deletes the pusher
                kind = null,
                appDisplayName = existing.app_display_name ?: "",
                deviceDisplayName = existing.device_display_name ?: "",
                profileTag = existing.profile_tag ?: "",
                lang = existing.lang,
                data = JsonPusherData(existing.data_url, existing.data_format),
                append = false
        )
        executeRequest<Unit>(eventBus) {
            apiCall = pushersAPI.setPusher(deleteBody)
        }
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            sessionDatabase.pushersQueries.deleteWithPushKey(params.pushKey)
        }
    }
}
