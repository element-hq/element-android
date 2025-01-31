/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.threads

/**
 * This class defines the state of a thread notification.
 */
enum class ThreadNotificationState {
    /**
     * There are no new message.
     */
    NO_NEW_MESSAGE,

    /**
     * There is at least one new message.
     */
    NEW_MESSAGE,

    /**
     * The is at least one new message that should be highlighted.
     * ex. "Hello @aris.kotsomitopoulos"
     */
    NEW_HIGHLIGHTED_MESSAGE;
}
