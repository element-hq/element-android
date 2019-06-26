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
package im.vector.matrix.android.internal.session.notification

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.types
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith


internal class BingRuleWatcher(monarchy: Monarchy,
                               private val task: ProcessEventForPushTask,
                               private val defaultPushRuleService: DefaultPushRuleService,
                               private val sessionParams: SessionParams,
                               private val taskExecutor: TaskExecutor) :
        RealmLiveEntityObserver<EventEntity>(monarchy) {

    override val query = Monarchy.Query<EventEntity> {

        EventEntity.types(it, listOf(
                EventType.MESSAGE,
                EventType.REDACTION,
                EventType.ENCRYPTED)
        )

    }

    override fun processChanges(inserted: List<EventEntity>, updated: List<EventEntity>, deleted: List<EventEntity>) {
        // TODO Use const for "global"
        val rules = defaultPushRuleService.getPushRules("global")
        inserted.map { it.asDomain() }
                .filter { it.senderId != sessionParams.credentials.userId }
                .let { events ->
                    task.configureWith(ProcessEventForPushTask.Params(events, rules))
                            .executeBy(taskExecutor)
                }
    }


}