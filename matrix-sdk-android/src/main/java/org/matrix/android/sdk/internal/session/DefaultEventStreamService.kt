/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session

import org.matrix.android.sdk.api.session.EventStreamService
import org.matrix.android.sdk.api.session.LiveEventListener
import javax.inject.Inject

internal class DefaultEventStreamService @Inject constructor(
        private val streamEventsManager: StreamEventsManager
) : EventStreamService {

    override fun addEventStreamListener(streamListener: LiveEventListener) {
        streamEventsManager.addLiveEventListener(streamListener)
    }

    override fun removeEventStreamListener(streamListener: LiveEventListener) {
        streamEventsManager.removeLiveEventListener(streamListener)
    }
}
