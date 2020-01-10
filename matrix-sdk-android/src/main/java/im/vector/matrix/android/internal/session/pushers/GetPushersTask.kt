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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetPushersTask : Task<Unit, Unit>

internal class DefaultGetPushersTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        private val monarchy: Monarchy,
        private val eventBus: EventBus
) : GetPushersTask {

    override suspend fun execute(params: Unit) {
        val response = executeRequest<GetPushersResponse>(eventBus) {
            apiCall = pushersAPI.getPushers()
        }
        monarchy.awaitTransaction { realm ->
            // clear existings?
            realm.where(PusherEntity::class.java)
                    .findAll().deleteAllFromRealm()
            response.pushers?.forEach { jsonPusher ->
                jsonPusher.toEntity().also {
                    it.state = PusherState.REGISTERED
                    realm.insertOrUpdate(it)
                }
            }
        }
    }
}
