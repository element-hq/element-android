/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.notification

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.pushrules.RuleScope
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

internal class DefaultSetRoomNotificationStateTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val removePushRuleTask: RemovePushRuleTask,
        private val addPushRuleTask: AddPushRuleTask
) :
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
