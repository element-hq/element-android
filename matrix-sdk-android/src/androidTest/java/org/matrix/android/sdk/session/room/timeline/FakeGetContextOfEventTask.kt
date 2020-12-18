/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.session.room.timeline

import org.matrix.android.sdk.internal.session.room.timeline.GetContextOfEventTask
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.room.timeline.TokenChunkEventPersistor
import kotlin.random.Random

internal class FakeGetContextOfEventTask constructor(private val tokenChunkEventPersistor: TokenChunkEventPersistor) : GetContextOfEventTask {

    override suspend fun execute(params: GetContextOfEventTask.Params): TokenChunkEventPersistor.Result {
        val fakeEvents = RoomDataHelper.createFakeListOfEvents(30)
        val tokenChunkEvent = FakeTokenChunkEvent(
                Random.nextLong(System.currentTimeMillis()).toString(),
                Random.nextLong(System.currentTimeMillis()).toString(),
                fakeEvents
        )
        return tokenChunkEventPersistor.insertInDb(tokenChunkEvent, params.roomId, PaginationDirection.BACKWARDS)
    }
}
