/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.location

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.internal.session.room.state.SendStateTask
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface StopLiveLocationShareTask : Task<StopLiveLocationShareTask.Params, UpdateLiveLocationShareResult> {
    data class Params(
            val roomId: String,
    )
}

internal class DefaultStopLiveLocationShareTask @Inject constructor(
        private val sendStateTask: SendStateTask,
        private val getActiveBeaconInfoForUserTask: GetActiveBeaconInfoForUserTask,
) : StopLiveLocationShareTask {

    override suspend fun execute(params: StopLiveLocationShareTask.Params): UpdateLiveLocationShareResult {
        val beaconInfoStateEvent = getActiveLiveLocationBeaconInfoForUser(params.roomId) ?: return getResultForIncorrectBeaconInfoEvent()
        val stateKey = beaconInfoStateEvent.stateKey ?: return getResultForIncorrectBeaconInfoEvent()
        val content = beaconInfoStateEvent.getClearContent()?.toModel<MessageBeaconInfoContent>() ?: return getResultForIncorrectBeaconInfoEvent()
        val updatedContent = content.copy(isLive = false).toContent()
        val sendStateTaskParams = SendStateTask.Params(
                roomId = params.roomId,
                stateKey = stateKey,
                eventType = EventType.STATE_ROOM_BEACON_INFO.unstable,
                body = updatedContent
        )
        return try {
            val eventId = sendStateTask.executeRetry(sendStateTaskParams, 3)
            if (eventId.isNotEmpty()) {
                UpdateLiveLocationShareResult.Success(eventId)
            } else {
                UpdateLiveLocationShareResult.Failure(Exception("empty event id for new state event"))
            }
        } catch (error: Throwable) {
            UpdateLiveLocationShareResult.Failure(error)
        }
    }

    private fun getResultForIncorrectBeaconInfoEvent() =
            UpdateLiveLocationShareResult.Failure(Exception("incorrect last beacon info event"))

    private suspend fun getActiveLiveLocationBeaconInfoForUser(roomId: String): Event? {
        val params = GetActiveBeaconInfoForUserTask.Params(
                roomId = roomId
        )
        return getActiveBeaconInfoForUserTask.execute(params)
    }
}
