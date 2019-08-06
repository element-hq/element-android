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
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.pushrules.rest.GetPushRulesResponse
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.android.internal.database.model.PushRulesEntity
import im.vector.matrix.android.internal.database.model.PusherEntityFields
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import javax.inject.Inject


internal interface GetPushRulesTask : Task<GetPushRulesTask.Params, Unit> {

    data class Params(val scope: String)

}


internal class DefaultGetPushRulesTask @Inject constructor(private val pushRulesApi: PushRulesApi,
                                                           private val monarchy: Monarchy,
                                                           private val sessionParams: SessionParams) : GetPushRulesTask {

    override suspend fun execute(params: GetPushRulesTask.Params) {
        val response = executeRequest<GetPushRulesResponse> {
            apiCall = pushRulesApi.getAllRules()
        }
        val scope = params.scope
        monarchy.awaitTransaction { realm ->
            //clear existings?
            //TODO
            realm.where(PushRulesEntity::class.java)
                    .equalTo(PusherEntityFields.USER_ID, sessionParams.credentials.userId)
                    .findAll().deleteAllFromRealm()

            val content = PushRulesEntity(sessionParams.credentials.userId, scope, "content")
            response.global.content?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    content.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(content)

            val override = PushRulesEntity(sessionParams.credentials.userId, scope, "override")
            response.global.override?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    override.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(override)

            val rooms = PushRulesEntity(sessionParams.credentials.userId, scope, "room")
            response.global.room?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    rooms.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(rooms)

            val senders = PushRulesEntity(sessionParams.credentials.userId, scope, "sender")
            response.global.sender?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    senders.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(senders)

            val underrides = PushRulesEntity(sessionParams.credentials.userId, scope, "underride")
            response.global.underride?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    underrides.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(underrides)
        }
    }
}