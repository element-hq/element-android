/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.send

enum class SendState {
    /**
     * The state is unknown.
     */
    UNKNOWN,

    /**
     * The event has not been sent.
     */
    UNSENT,

    /**
     * The event is encrypting.
     */
    ENCRYPTING,

    /**
     * The event is currently sending.
     */
    SENDING,

    /**
     * The event has been sent.
     */
    SENT,

    /**
     * The event has been received from server.
     */
    SYNCED,

    /**
     * The event failed to be sent.
     */
    UNDELIVERED,

    /**
     * The event failed to be sent because some unknown devices have been found while encrypting it.
     */
    FAILED_UNKNOWN_DEVICES;

    internal companion object {
        val HAS_FAILED_STATES = listOf(UNDELIVERED, FAILED_UNKNOWN_DEVICES)
        val IS_SENT_STATES = listOf(SENT, SYNCED)
        val IS_PROGRESSING_STATES = listOf(ENCRYPTING, SENDING)
        val IS_SENDING_STATES = IS_PROGRESSING_STATES + UNSENT
        val PENDING_STATES = IS_SENDING_STATES + HAS_FAILED_STATES
    }

    fun isSent() = IS_SENT_STATES.contains(this)

    fun hasFailed() = HAS_FAILED_STATES.contains(this)

    fun isInProgress() = IS_PROGRESSING_STATES.contains(this)

    fun isSending() = IS_SENDING_STATES.contains(this)
}
