/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.widgets

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.query.whereStateKey
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface CreateWidgetTask : Task<CreateWidgetTask.Params, String> {

    data class Params(
            val roomId: String,
            val widgetId: String,
            val content: Content
    )
}

internal class DefaultCreateWidgetTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val roomAPI: RoomAPI,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : CreateWidgetTask {

    override suspend fun execute(params: CreateWidgetTask.Params): String {
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.sendStateEvent(
                    roomId = params.roomId,
                    stateEventType = EventType.STATE_ROOM_WIDGET_LEGACY,
                    stateKey = params.widgetId,
                    params = params.content
            )
        }
        awaitNotEmptyResult(monarchy.realmConfiguration, 30_000L) {
            CurrentStateEventEntity
                    .whereStateKey(it, params.roomId, type = EventType.STATE_ROOM_WIDGET_LEGACY, stateKey = params.widgetId)
                    .and()
                    .equalTo(CurrentStateEventEntityFields.ROOT.SENDER, userId)
        }
        return response.eventId.also {
            Timber.d("Widget state event: $it just sent in room ${params.roomId}")
        }
    }
}
