/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptsItem
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptsItem_
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import javax.inject.Inject

class ReadReceiptsItemFactory @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val session: Session
) {

    fun create(
            eventId: String,
            readReceipts: List<ReadReceipt>,
            callback: TimelineEventController.Callback?,
            isFromThreadTimeLine: Boolean,
    ): ReadReceiptsItem? {
        if (readReceipts.isEmpty()) {
            return null
        }
        val readReceiptsData = readReceipts
                .map {
                    ReadReceiptData(it.roomMember.userId, it.roomMember.avatarUrl, it.roomMember.displayName, it.originServerTs)
                }
                .sortedByDescending { it.timestamp }
        val threadReadReceiptsSupported = session.homeServerCapabilitiesService().getHomeServerCapabilities().canUseThreadReadReceiptsAndNotifications
        return ReadReceiptsItem_()
                .id("read_receipts_$eventId")
                .eventId(eventId)
                .readReceipts(readReceiptsData)
                .avatarRenderer(avatarRenderer)
                .shouldHideReadReceipts(isFromThreadTimeLine && !threadReadReceiptsSupported)
                .clickListener {
                    callback?.onReadReceiptsClicked(readReceiptsData)
                }
    }
}
