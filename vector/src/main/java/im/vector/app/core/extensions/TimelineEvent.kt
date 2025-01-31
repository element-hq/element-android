/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun TimelineEvent.canReact(): Boolean {
    // Only event of type EventType.MESSAGE, EventType.STICKER and EventType.POLL_START are supported for the moment
    return root.getClearType() in listOf(EventType.MESSAGE, EventType.STICKER) + EventType.POLL_START &&
            root.sendState == SendState.SYNCED &&
            !root.isRedacted()
}
