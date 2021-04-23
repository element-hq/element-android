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
package org.matrix.android.sdk.internal.session.room.relation

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface FetchEditHistoryTask : Task<FetchEditHistoryTask.Params, List<Event>> {
    data class Params(
            val roomId: String,
            val eventId: String
    )
}

internal class DefaultFetchEditHistoryTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider
) : FetchEditHistoryTask {

    override suspend fun execute(params: FetchEditHistoryTask.Params): List<Event> {
        val isRoomEncrypted = cryptoSessionInfoProvider.isRoomEncrypted(params.roomId)
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.getRelations(
                    roomId = params.roomId,
                    eventId = params.eventId,
                    relationType = RelationType.REPLACE,
                    eventType = if (isRoomEncrypted) EventType.ENCRYPTED else EventType.MESSAGE
            )
        }

        // Filter out edition form other users, and redacted editions
        val originalSenderId = response.originalEvent?.senderId
        val events = response.chunks
                .filter { it.senderId == originalSenderId }
                .filter { !it.isRedacted() }
        return events + listOfNotNull(response.originalEvent)
    }
}
