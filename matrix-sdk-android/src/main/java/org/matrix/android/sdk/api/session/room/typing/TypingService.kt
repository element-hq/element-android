/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.typing

/**
 * This interface defines methods to handle typing data. It's implemented at the room level.
 */
interface TypingService {

    /**
     * To call when user is typing a message in the room.
     * The SDK will handle the requests scheduling to the homeserver:
     * - No more than one typing request per 10s
     * - If not called after 10s, the SDK will notify the homeserver that the user is not typing anymore
     */
    fun userIsTyping()

    /**
     * To call when user stops typing in the room
     * Notify immediately the homeserver that the user is not typing anymore in the room, for
     * instance when user has emptied the composer, or when the user quits the timeline screen.
     */
    fun userStopsTyping()
}
