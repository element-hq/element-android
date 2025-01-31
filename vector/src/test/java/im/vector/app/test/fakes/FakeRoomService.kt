/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
}
