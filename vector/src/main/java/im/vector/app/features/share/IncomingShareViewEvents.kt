/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class IncomingShareViewEvents : VectorViewEvents {
    data class ShareToRoom(
            val roomSummary: RoomSummary,
            val sharedData: SharedData,
            val showAlert: Boolean
    ) : IncomingShareViewEvents()

    data class EditMediaBeforeSending(val contentAttachmentData: List<ContentAttachmentData>) : IncomingShareViewEvents()
    data class MultipleRoomsShareDone(val roomId: String) : IncomingShareViewEvents()
}
