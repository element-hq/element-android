/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.readreceipts

import im.vector.lib.core.utils.compat.removeIfCompat
import org.matrix.android.sdk.api.session.room.model.ReadReceipt

class ReadReceiptsCache {

    private val receiptsByEventId = HashMap<String, MutableList<ReadReceipt>>()

    // Key is userId, Value is eventId
    private val receiptEventIdByUserId = HashMap<String, String>()

    fun receiptsByEvent(): Map<String, List<ReadReceipt>> {
        return receiptsByEventId
    }

    fun addReceiptsOnEvent(receipts: List<ReadReceipt>, eventId: String) {
        val existingReceipts = receiptsByEventId.getOrPut(eventId) { ArrayList() }
        receipts.forEach { readReceipt ->
            val receiptUserId = readReceipt.roomMember.userId
            val receiptEventId = receiptEventIdByUserId[receiptUserId]
            // If we already have a read receipt for this user, move it so we only
            // use the most recent. It can happen because of threaded read receipts.
            if (receiptEventId != null) {
                receiptsByEventId[receiptEventId]?.removeIfCompat {
                    it.roomMember.userId == receiptUserId
                }
            }
            receiptEventIdByUserId[receiptUserId] = eventId
            existingReceipts.add(readReceipt)
        }
    }

    fun clear() {
        receiptsByEventId.clear()
        receiptEventIdByUserId.clear()
    }
}
