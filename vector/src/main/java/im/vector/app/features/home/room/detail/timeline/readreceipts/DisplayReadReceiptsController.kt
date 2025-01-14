/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.readreceipts

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.item.toMatrixItem
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Epoxy controller for read receipt event list.
 */
class DisplayReadReceiptsController @Inject constructor(
        private val dateFormatter: VectorDateFormatter,
        private val session: Session,
        private val avatarRender: AvatarRenderer
) :
        TypedEpoxyController<List<ReadReceiptData>>() {

    var listener: Listener? = null

    override fun buildModels(readReceipts: List<ReadReceiptData>) {
        readReceipts.forEach { readReceiptData ->
            val timestamp = dateFormatter.format(readReceiptData.timestamp, DateFormatKind.DEFAULT_DATE_AND_TIME)
            DisplayReadReceiptItem_()
                    .id(readReceiptData.userId)
                    .matrixItem(readReceiptData.toMatrixItem())
                    .avatarRenderer(avatarRender)
                    .timestamp(timestamp)
                    .userClicked { listener?.didSelectUser(readReceiptData.userId) }
                    .addIf(session.myUserId != readReceiptData.userId, this)
        }
    }

    interface Listener {
        fun didSelectUser(userId: String)
    }
}
