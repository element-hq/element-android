/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.settings.joinrule.advanced

import im.vector.app.core.platform.ViewModelTask
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

sealed class ChangeJoinRuleTaskResult {

    object Success : ChangeJoinRuleTaskResult()

    data class Failure(val error: Throwable) : ChangeJoinRuleTaskResult()
}

data class ChangeJoinRuleParams(
        val roomId: String,
        val newJoinRule: RoomJoinRules,
        val newAllowIfRestricted: List<String>? = null
)

class ChangeJoinRuleViewModelTask @Inject constructor(
        private val session: Session
) : ViewModelTask<ChangeJoinRuleParams, ChangeJoinRuleTaskResult> {

    override suspend fun execute(params: ChangeJoinRuleParams): ChangeJoinRuleTaskResult {
        val room = session.getRoom(params.roomId) ?: return ChangeJoinRuleTaskResult.Failure(IllegalArgumentException("Unknown room"))

        try {
            when (params.newJoinRule) {
                RoomJoinRules.PUBLIC,
                RoomJoinRules.INVITE     -> {
                    room.updateJoinRule(params.newJoinRule, null)
                }
                RoomJoinRules.RESTRICTED -> updateRestrictedJoinRule(params.roomId, params.newAllowIfRestricted.orEmpty())
                RoomJoinRules.KNOCK,
                RoomJoinRules.PRIVATE    -> {
                    return ChangeJoinRuleTaskResult.Failure(UnsupportedOperationException())
                }
            }
        } catch (failure: Throwable) {
            return ChangeJoinRuleTaskResult.Failure(failure)
        }

        return ChangeJoinRuleTaskResult.Success
    }

    fun updateRestrictedJoinRule(roomId: String, allowList: List<String>) {
        // let's compute correct via list
        allowList.map {
            session.getRoomSummary(it)
        }
    }
}
