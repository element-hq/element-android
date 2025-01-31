/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.notification

/**
 * Defines the room notification state.
 */
enum class RoomNotificationState {
    /**
     * All the messages will trigger a noisy notification.
     */
    ALL_MESSAGES_NOISY,

    /**
     * All the messages will trigger a notification.
     */
    ALL_MESSAGES,

    /**
     * Only the messages with user display name / user name will trigger notifications.
     */
    MENTIONS_ONLY,

    /**
     * No notifications.
     */
    MUTE
}
