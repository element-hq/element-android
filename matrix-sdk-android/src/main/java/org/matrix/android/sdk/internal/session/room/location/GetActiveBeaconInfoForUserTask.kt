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
        return EventType.STATE_ROOM_BEACON_INFO.values
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
