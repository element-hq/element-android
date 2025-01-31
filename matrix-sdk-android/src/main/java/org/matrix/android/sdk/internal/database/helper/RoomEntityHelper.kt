/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.helper

import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity

internal fun RoomEntity.addIfNecessary(chunkEntity: ChunkEntity) {
    if (!chunks.contains(chunkEntity)) {
        chunks.add(chunkEntity)
    }
}

internal fun RoomEntity.addIfNecessary(threadSummary: ThreadSummaryEntity) {
    if (!threadSummaries.contains(threadSummary)) {
        threadSummaries.add(threadSummary)
    }
}
