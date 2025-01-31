/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.location

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetActiveBeaconInfoForUserTask : Task<GetActiveBeaconInfoForUserTask.Params, Event?> {
    data class Params(
            val roomId: String,
    )
}

internal class DefaultGetActiveBeaconInfoForUserTask @Inject constructor(
        @UserId private val userId: String,
        private val stateEventDataSource: StateEventDataSource,
) : GetActiveBeaconInfoForUserTask {

    override suspend fun execute(params: GetActiveBeaconInfoForUserTask.Params): Event? {
        return EventType.STATE_ROOM_BEACON_INFO
                .mapNotNull {
                    stateEventDataSource.getStateEvent(
                            roomId = params.roomId,
                            eventType = it,
                            stateKey = QueryStringValue.Equals(userId)
                    )
                }
                .firstOrNull { beaconInfoEvent ->
                    beaconInfoEvent.getClearContent()?.toModel<MessageBeaconInfoContent>()?.isLive.orFalse()
                }
    }
}
