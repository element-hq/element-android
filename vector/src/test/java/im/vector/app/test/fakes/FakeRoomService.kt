/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.room.model.RoomSummary

class FakeRoomService(
        private val fakeRoom: FakeRoom = FakeRoom()
) : RoomService by mockk() {

    override fun getRoom(roomId: String) = fakeRoom

    fun getRoomSummaryReturns(roomSummary: RoomSummary?) {
        every { getRoomSummary(any()) } returns roomSummary
    }

    fun set(roomSummary: RoomSummary?) {
        every { getRoomSummary(any()) } returns roomSummary
    }
}
