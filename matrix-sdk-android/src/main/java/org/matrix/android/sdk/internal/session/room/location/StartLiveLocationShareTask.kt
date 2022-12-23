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

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.state.SendStateTask
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal interface StartLiveLocationShareTask : Task<StartLiveLocationShareTask.Params, UpdateLiveLocationShareResult> {
    data class Params(
            val roomId: String,
            val timeoutMillis: Long,
    )
}

internal class DefaultStartLiveLocationShareTask @Inject constructor(
        @UserId private val userId: String,
        private val clock: Clock,
        private val sendStateTask: SendStateTask,
) : StartLiveLocationShareTask {

    override suspend fun execute(params: StartLiveLocationShareTask.Params): UpdateLiveLocationShareResult {
        val beaconContent = MessageBeaconInfoContent(
                body = "Live location",
                timeout = params.timeoutMillis,
                isLive = true,
                unstableTimestampMillis = clock.epochMillis()
        ).toContent()
        val eventType = EventType.STATE_ROOM_BEACON_INFO.unstable
        val sendStateTaskParams = SendStateTask.Params(
                roomId = params.roomId,
                stateKey = userId,
                eventType = eventType,
                body = beaconContent
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
}
