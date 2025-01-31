/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.threads

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

/**
 * This class contains all the details needed for threads.
 * Is is mainly used from within an Event.
 */
data class ThreadDetails(
        val isRootThread: Boolean = false,
        val numberOfThreads: Int = 0,
        val threadSummarySenderInfo: SenderInfo? = null,
        val threadSummaryLatestEvent: Event? = null,
        val lastMessageTimestamp: Long? = null,
        val threadNotificationState: ThreadNotificationState = ThreadNotificationState.NO_NEW_MESSAGE,
        val isThread: Boolean = false,
        val lastRootThreadEdition: String? = null
)
