/*
 * Copyright (c) 2023 New Vector Ltd
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
