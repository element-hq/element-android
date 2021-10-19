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

package org.matrix.android.sdk.internal.session.room.notification

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.pushrules.RuleScope
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.internal.database.model.PushRuleEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.pushers.AddPushRuleTask
import org.matrix.android.sdk.internal.session.pushers.RemovePushRuleTask
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SetRoomNotificationStateTask : Task<SetRoomNotificationStateTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val roomNotificationState: RoomNotificationState
    )
}

internal class DefaultSetRoomNotificationStateTask @Inject constructor(@SessionDatabase private val monarchy: Monarchy,
                                                                       private val removePushRuleTask: RemovePushRuleTask,
                                                                       private val addPushRuleTask: AddPushRuleTask) :
    SetRoomNotificationStateTask {

    override suspend fun execute(params: SetRoomNotificationStateTask.Params) {
        val currentRoomPushRule = Realm.getInstance(monarchy.realmConfiguration).use {
            PushRuleEntity.where(it, scope = RuleScope.GLOBAL, ruleId = params.roomId).findFirst()?.toRoomPushRule()
        }
        if (currentRoomPushRule != null) {
            removePushRuleTask.execute(RemovePushRuleTask.Params(currentRoomPushRule.kind, currentRoomPushRule.rule.ruleId))
        }
        val newRoomPushRule = params.roomNotificationState.toRoomPushRule(params.roomId)
        if (newRoomPushRule != null) {
            addPushRuleTask.execute(AddPushRuleTask.Params(newRoomPushRule.kind, newRoomPushRule.rule))
        }
    }
}
