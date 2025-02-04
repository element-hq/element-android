/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.session.room.model.RoomSummary

object RoomSummaryFixture {

    fun aRoomSummary(roomId: String) = RoomSummary(
            roomId,
            isEncrypted = false,
            encryptionEventTs = 0,
            typingUsers = emptyList(),
    )
}
