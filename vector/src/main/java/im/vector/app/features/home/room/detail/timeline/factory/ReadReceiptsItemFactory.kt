/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptsItem
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptsItem_
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import javax.inject.Inject

class ReadReceiptsItemFactory @Inject constructor(private val avatarRenderer: AvatarRenderer) {

    fun create(
            eventId: String,
            readReceipts: List<ReadReceipt>,
            callback: TimelineEventController.Callback?,
            isFromThreadTimeLine: Boolean): ReadReceiptsItem? {
        if (readReceipts.isEmpty()) {
            return null
        }
        val readReceiptsData = readReceipts
                .map {
                    ReadReceiptData(it.roomMember.userId, it.roomMember.avatarUrl, it.roomMember.displayName, it.originServerTs)
                }
                .sortedByDescending { it.timestamp }
        return ReadReceiptsItem_()
                .id("read_receipts_$eventId")
                .eventId(eventId)
                .readReceipts(readReceiptsData)
                .avatarRenderer(avatarRenderer)
                .shouldHideReadReceipts(isFromThreadTimeLine)
                .clickListener {
                    callback?.onReadReceiptsClicked(readReceiptsData)
                }
    }
}
