/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
                eventType = EventType.STATE_ROOM_BEACON_INFO.first(),
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
