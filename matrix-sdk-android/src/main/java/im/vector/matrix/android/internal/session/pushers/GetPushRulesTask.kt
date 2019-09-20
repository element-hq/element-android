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
import im.vector.matrix.android.api.pushrules.RuleSetKey
import im.vector.matrix.android.api.pushrules.rest.GetPushRulesResponse
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.android.internal.database.model.PushRulesEntity
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import javax.inject.Inject


internal interface GetPushRulesTask : Task<GetPushRulesTask.Params, Unit> {
    data class Params(val scope: String)
}

internal class DefaultGetPushRulesTask @Inject constructor(private val pushRulesApi: PushRulesApi,
                                                           private val monarchy: Monarchy) : GetPushRulesTask {

    override suspend fun execute(params: GetPushRulesTask.Params) {
        val response = executeRequest<GetPushRulesResponse> {
            apiCall = pushRulesApi.getAllRules()
        }
        val scope = params.scope
        monarchy.awaitTransaction { realm ->
            //clear existings?
            //TODO
            realm.where(PushRulesEntity::class.java)
                    .findAll()
                    .deleteAllFromRealm()

            val content = PushRulesEntity(scope).apply { kind = RuleSetKey.CONTENT }
            response.global.content?.forEach { rule ->
                content.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(content)

            val override = PushRulesEntity(scope).apply { kind = RuleSetKey.OVERRIDE }
            response.global.override?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    override.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(override)

            val rooms = PushRulesEntity(scope).apply { kind = RuleSetKey.ROOM }
            response.global.room?.forEach { rule ->
                rooms.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(rooms)

            val senders = PushRulesEntity(scope).apply { kind = RuleSetKey.SENDER }
            response.global.sender?.forEach { rule ->
                senders.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(senders)

            val underrides = PushRulesEntity(scope).apply { kind = RuleSetKey.UNDERRIDE }
            response.global.underride?.forEach { rule ->
                underrides.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(underrides)
        }
    }
}