/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.summary

import org.matrix.android.sdk.api.session.events.model.EventType

object RoomSummaryConstants {

    /**
     *
     */
    val PREVIEWABLE_TYPES = listOf(
            // TODO filter message type (KEY_VERIFICATION_READY, etc.)
            EventType.MESSAGE,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_REJECT,
            EventType.CALL_ANSWER,
            EventType.ENCRYPTED,
            EventType.STICKER,
            EventType.REACTION
    ) + EventType.POLL_START + EventType.STATE_ROOM_BEACON_INFO
}
