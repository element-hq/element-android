/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import javax.inject.Inject

internal class ThreadSummaryMapper @Inject constructor() {

    fun map(threadSummary: ThreadSummaryEntity): ThreadSummary {
        return ThreadSummary(
                roomId = threadSummary.room?.firstOrNull()?.roomId.orEmpty(),
                rootEvent = threadSummary.rootThreadEventEntity?.asDomain(),
                latestEvent = threadSummary.latestThreadEventEntity?.asDomain(),
                rootEventId = threadSummary.rootThreadEventId.orEmpty(),
                rootThreadSenderInfo = SenderInfo(
                        userId = threadSummary.rootThreadEventEntity?.sender ?: "",
                        displayName = threadSummary.rootThreadSenderName,
                        isUniqueDisplayName = threadSummary.rootThreadIsUniqueDisplayName,
                        avatarUrl = threadSummary.rootThreadSenderAvatar
                ),
                latestThreadSenderInfo = SenderInfo(
                        userId = threadSummary.latestThreadEventEntity?.sender ?: "",
                        displayName = threadSummary.latestThreadSenderName,
                        isUniqueDisplayName = threadSummary.latestThreadIsUniqueDisplayName,
                        avatarUrl = threadSummary.latestThreadSenderAvatar
                ),
                isUserParticipating = threadSummary.isUserParticipating,
                numberOfThreads = threadSummary.numberOfThreads
        )
    }
}
