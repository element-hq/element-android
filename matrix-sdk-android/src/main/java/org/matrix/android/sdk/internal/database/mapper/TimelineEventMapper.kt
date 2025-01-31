/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import javax.inject.Inject

internal class TimelineEventMapper @Inject constructor(private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper) {

    fun map(timelineEventEntity: TimelineEventEntity, buildReadReceipts: Boolean = true): TimelineEvent {
        val readReceipts = if (buildReadReceipts) {
            timelineEventEntity.readReceipts
                    ?.let {
                        readReceiptsSummaryMapper.map(it)
                    }
        } else {
            null
        }
        return TimelineEvent(
                root = timelineEventEntity.root?.asDomain()
                        ?: Event("", timelineEventEntity.eventId),
                eventId = timelineEventEntity.eventId,
                annotations = timelineEventEntity.annotations?.asDomain(),
                localId = timelineEventEntity.localId,
                displayIndex = timelineEventEntity.displayIndex,
                senderInfo = SenderInfo(
                        userId = timelineEventEntity.root?.sender ?: "",
                        displayName = timelineEventEntity.senderName,
                        isUniqueDisplayName = timelineEventEntity.isUniqueDisplayName,
                        avatarUrl = timelineEventEntity.senderAvatar
                ),
                ownedByThreadChunk = timelineEventEntity.ownedByThreadChunk,
                readReceipts = readReceipts
                        ?.distinctBy {
                            it.roomMember
                        }?.sortedByDescending {
                            it.originServerTs
                        }.orEmpty()
        )
    }
}
