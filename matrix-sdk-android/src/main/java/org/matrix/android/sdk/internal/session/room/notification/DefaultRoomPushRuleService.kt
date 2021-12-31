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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.pushrules.RuleScope
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.session.room.notification.RoomPushRuleService
import org.matrix.android.sdk.internal.database.model.PushRuleEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase

internal class DefaultRoomPushRuleService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                      private val setRoomNotificationStateTask: SetRoomNotificationStateTask,
                                                                      @SessionDatabase private val monarchy: Monarchy) :
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
