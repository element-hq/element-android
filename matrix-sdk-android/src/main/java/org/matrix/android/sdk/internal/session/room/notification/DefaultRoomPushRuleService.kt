/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.pushrules.RuleScope
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.session.room.notification.RoomPushRuleService
import org.matrix.android.sdk.internal.database.model.PushRuleEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase

internal class DefaultRoomPushRuleService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val setRoomNotificationStateTask: SetRoomNotificationStateTask,
        @SessionDatabase private val monarchy: Monarchy
) :
        RoomPushRuleService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultRoomPushRuleService
    }

    override fun getLiveRoomNotificationState(): LiveData<RoomNotificationState> {
        return Transformations.map(getPushRuleForRoom()) {
            it?.toRoomNotificationState() ?: RoomNotificationState.ALL_MESSAGES
        }
    }

    override suspend fun setRoomNotificationState(roomNotificationState: RoomNotificationState) {
        setRoomNotificationStateTask.execute(SetRoomNotificationStateTask.Params(roomId, roomNotificationState))
    }

    private fun getPushRuleForRoom(): LiveData<RoomPushRule?> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm ->
                    PushRuleEntity.where(realm, scope = RuleScope.GLOBAL, ruleId = roomId)
                },
                { result ->
                    result.toRoomPushRule()
                }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull()
        }
    }
}
