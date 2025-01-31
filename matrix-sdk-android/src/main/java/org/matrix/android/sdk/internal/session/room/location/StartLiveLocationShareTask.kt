/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            val description: String,
    )
}

internal class DefaultStartLiveLocationShareTask @Inject constructor(
        @UserId private val userId: String,
        private val clock: Clock,
        private val sendStateTask: SendStateTask,
) : StartLiveLocationShareTask {

    override suspend fun execute(params: StartLiveLocationShareTask.Params): UpdateLiveLocationShareResult {
        val beaconContent = MessageBeaconInfoContent(
                body = params.description,
                timeout = params.timeoutMillis,
                isLive = true,
                unstableTimestampMillis = clock.epochMillis()
        ).toContent()
        val eventType = EventType.STATE_ROOM_BEACON_INFO.first()
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
