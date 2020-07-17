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

package im.vector.matrix.android.internal.session.room.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.pushrules.RuleScope
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.matrix.android.api.session.room.notification.RoomPushRuleService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.model.PushRuleEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

internal class DefaultRoomPushRuleService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                      private val setRoomNotificationStateTask: SetRoomNotificationStateTask,
                                                                      @SessionDatabase private val monarchy: Monarchy,
                                                                      private val taskExecutor: TaskExecutor)
    : RoomPushRuleService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): RoomPushRuleService
    }

    override fun getLiveRoomNotificationState(): LiveData<RoomNotificationState> {
        return Transformations.map(getPushRuleForRoom()) {
            it?.toRoomNotificationState() ?: RoomNotificationState.ALL_MESSAGES
        }
    }

    override fun setRoomNotificationState(roomNotificationState: RoomNotificationState, matrixCallback: MatrixCallback<Unit>): Cancelable {
        return setRoomNotificationStateTask
                .configureWith(SetRoomNotificationStateTask.Params(roomId, roomNotificationState)) {
                    this.callback = matrixCallback
                }
                .executeBy(taskExecutor)
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
