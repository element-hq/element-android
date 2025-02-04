/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.config

enum class KeySharingStrategy {
    /**
     * Keys will be sent for the first time when the first message is sent.
     * This is handled by the Matrix SDK so there's no need to do it in Vector.
     */
    WhenSendingEvent,

    /**
     * Keys will be sent for the first time when the timeline displayed.
     */
    WhenEnteringRoom,

    /**
     * Keys will be sent for the first time when a typing started.
     */
    WhenTyping
}
