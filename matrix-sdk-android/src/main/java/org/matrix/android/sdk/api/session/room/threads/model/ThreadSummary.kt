/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.threads.model

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

/**
 * The main thread Summary model, mainly used to display the thread list.
 */
data class ThreadSummary(
        val roomId: String,
        val rootEvent: Event?,
        val latestEvent: Event?,
        val rootEventId: String,
        val rootThreadSenderInfo: SenderInfo,
        val latestThreadSenderInfo: SenderInfo,
        val isUserParticipating: Boolean,
        val numberOfThreads: Int,
        val threadEditions: ThreadEditions = ThreadEditions()
)
