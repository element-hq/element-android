/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.matrix.android.sdk.api.session.room.model.LocalRoomSummary
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource

internal class FakeRoomSummaryDataSource {

    val instance: RoomSummaryDataSource = mockk()

    fun givenGetLocalRoomSummaryReturns(roomId: String?, localRoomSummary: LocalRoomSummary?) {
        every { instance.getLocalRoomSummary(roomId = roomId ?: any()) } returns localRoomSummary
    }

    fun verifyGetLocalRoomSummary(roomId: String) {
        verify { instance.getLocalRoomSummary(roomId) }
    }
}
