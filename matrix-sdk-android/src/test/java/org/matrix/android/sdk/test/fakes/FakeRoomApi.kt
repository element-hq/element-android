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

package org.matrix.android.sdk.test.fakes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.relation.RelationsResponse

internal class FakeRoomApi {

    val instance: RoomAPI = mockk()

    fun givenGetRelationsReturns(
            from: String?,
            relationsResponse: RelationsResponse,
    ) {
        coEvery {
            instance.getRelations(
                    roomId = any(),
                    eventId = any(),
                    relationType = any(),
                    from = from,
                    limit = any()
            )
        } returns relationsResponse
    }

    fun verifyGetRelations(
            roomId: String,
            eventId: String,
            relationType: String,
            from: String?,
            limit: Int,
    ) {
        coVerify {
            instance.getRelations(
                    roomId = roomId,
                    eventId = eventId,
                    relationType = relationType,
                    from = from,
                    limit = limit
            )
        }
    }
}
