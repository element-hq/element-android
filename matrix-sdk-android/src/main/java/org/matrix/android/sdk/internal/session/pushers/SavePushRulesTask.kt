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

import org.matrix.android.sdk.api.session.pushrules.RuleScope
import org.matrix.android.sdk.api.session.pushrules.RuleSetKey
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.PushRulesMapper
import org.matrix.android.sdk.internal.database.model.PushRulesEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

/**
 * Save the push rules in DB.
 */
internal interface SavePushRulesTask : Task<SavePushRulesTask.Params, Unit> {
    data class Params(val pushRules: GetPushRulesResponse)
}

internal class DefaultSavePushRulesTask @Inject constructor(@SessionDatabase private val realmInstance: RealmInstance) : SavePushRulesTask {

    override suspend fun execute(params: SavePushRulesTask.Params) {
        realmInstance.write {
            // clear current push rules
            query(PushRulesEntity::class)
                    .find()
                    .forEach {
                        deleteOnCascade(it)
                    }

            // Save only global rules for the moment
            val globalRules = params.pushRules.global

            val content = PushRulesEntity().apply {
                scope = RuleScope.GLOBAL
                kind = RuleSetKey.CONTENT
            }
            globalRules.content?.forEach { rule ->
                content.pushRules.add(PushRulesMapper.map(rule))
            }
            copyToRealm(content)

            val override = PushRulesEntity().apply {
                scope = RuleScope.GLOBAL
                kind = RuleSetKey.OVERRIDE
            }
            globalRules.override?.forEach { rule ->
                PushRulesMapper.map(rule).also {
                    override.pushRules.add(it)
                }
            }
            copyToRealm(override)

            val rooms = PushRulesEntity().apply {
                scope = RuleScope.GLOBAL
                kind = RuleSetKey.ROOM
            }
            globalRules.room?.forEach { rule ->
                rooms.pushRules.add(PushRulesMapper.map(rule))
            }
            copyToRealm(rooms)

            val senders = PushRulesEntity().apply {
                scope = RuleScope.GLOBAL
                kind = RuleSetKey.SENDER
            }
            globalRules.sender?.forEach { rule ->
                senders.pushRules.add(PushRulesMapper.map(rule))
            }
            copyToRealm(senders)

            val underrides = PushRulesEntity().apply {
                scope = RuleScope.GLOBAL
                kind = RuleSetKey.UNDERRIDE
            }
            globalRules.underride?.forEach { rule ->
                underrides.pushRules.add(PushRulesMapper.map(rule))
            }
            copyToRealm(underrides)
        }
    }
}
