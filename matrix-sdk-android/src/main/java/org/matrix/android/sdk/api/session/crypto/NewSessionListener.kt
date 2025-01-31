/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto

/**
 * This listener notifies on new Megolm sessions being created.
 */
interface NewSessionListener {

    /**
     * @param roomId the room id where the new Megolm session has been created for, may be null when importing from external sessions
     * @param senderKey the sender key of the device which the Megolm session is shared with
     * @param sessionId the session id of the Megolm session
     */
    fun onNewSession(roomId: String?, senderKey: String, sessionId: String)
}
