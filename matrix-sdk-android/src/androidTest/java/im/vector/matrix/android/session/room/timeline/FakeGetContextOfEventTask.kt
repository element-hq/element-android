/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.session.room.timeline

import arrow.core.Try
import im.vector.matrix.android.internal.session.room.timeline.GetContextOfEventTask
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEvent
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import kotlin.random.Random

internal class FakeGetContextOfEventTask(private val tokenChunkEventPersistor: TokenChunkEventPersistor) : GetContextOfEventTask {

    override fun execute(params: GetContextOfEventTask.Params): Try<TokenChunkEventPersistor.Result> {
        val fakeEvents = RoomDataHelper.createFakeListOfEvents(30)
        val tokenChunkEvent = FakeTokenChunkEvent(
                Random.nextLong(System.currentTimeMillis()).toString(),
                Random.nextLong(System.currentTimeMillis()).toString(),
                fakeEvents
        )
        return tokenChunkEventPersistor.insertInDb(tokenChunkEvent, params.roomId, PaginationDirection.BACKWARDS)
    }


}