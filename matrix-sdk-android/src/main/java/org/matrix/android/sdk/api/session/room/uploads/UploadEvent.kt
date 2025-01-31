/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.uploads

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

/**
 * Wrapper around on Event.
 * Similar to [org.matrix.android.sdk.api.session.room.timeline.TimelineEvent], contains an Event with extra useful data
 */
data class UploadEvent(
        val root: Event,
        val eventId: String,
        val contentWithAttachmentContent: MessageWithAttachmentContent,
        val senderInfo: SenderInfo
)
