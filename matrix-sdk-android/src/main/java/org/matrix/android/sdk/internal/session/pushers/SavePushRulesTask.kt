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
package org.matrix.android.sdk.internal.session.pushers

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.pushrules.RuleScope
import org.matrix.android.sdk.api.pushrules.RuleSetKey
import org.matrix.android.sdk.api.pushrules.rest.GetPushRulesResponse
import org.matrix.android.sdk.internal.database.mapper.PushRulesMapper
import org.matrix.android.sdk.internal.database.model.PushRulesEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

/**
 * Save the push rules in DB
 */
internal interface SavePushRulesTask : Task<SavePushRulesTask.Params, Unit> {
    data class Params(val pushRules: GetPushRulesResponse)
}

internal class DefaultSavePushRulesTask @Inject constructor(@SessionDatabase private val monarchy: Monarchy) : SavePushRulesTask {

    override suspend fun execute(params: SavePushRulesTask.Params) {
        monarchy.awaitTransaction { realm ->
            // clear current push rules
            realm.where(PushRulesEntity::class.java)
                    .findAll()
                    .forEach { it.deleteOnCascade() }

            // Save only global rules for the moment
            val globalRules = params.pushRules.global

            val content = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.CONTENT }
            globalRules.content?.forEach { rule ->
                content.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(content)

            val override = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.OVERRIDE }
            globalRules.override?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    override.pushRules.add(it)
                }
            }
            realm.insertOrUpdate(override)

            val rooms = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.ROOM }
            globalRules.room?.forEach { rule ->
                rooms.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(rooms)

            val senders = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.SENDER }
            globalRules.sender?.forEach { rule ->
                senders.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(senders)

            val underrides = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.UNDERRIDE }
            globalRules.underride?.forEach { rule ->
                underrides.pushRules.add(PushRulesMapper.map(rule))
            }
            realm.insertOrUpdate(underrides)
        }
    }
}
