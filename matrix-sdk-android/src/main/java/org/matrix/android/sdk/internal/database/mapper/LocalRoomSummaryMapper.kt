/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.model.LocalRoomSummary
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import javax.inject.Inject

internal class LocalRoomSummaryMapper @Inject constructor(
        private val roomSummaryMapper: RoomSummaryMapper,
) {

    fun map(localRoomSummaryEntity: LocalRoomSummaryEntity): LocalRoomSummary {
        return LocalRoomSummary(
                roomId = localRoomSummaryEntity.roomId,
                roomSummary = localRoomSummaryEntity.roomSummaryEntity?.let { roomSummaryMapper.map(it) },
                createRoomParams = localRoomSummaryEntity.createRoomParams,
                replacementRoomId = localRoomSummaryEntity.replacementRoomId,
                creationState = localRoomSummaryEntity.creationState
        )
    }
}
