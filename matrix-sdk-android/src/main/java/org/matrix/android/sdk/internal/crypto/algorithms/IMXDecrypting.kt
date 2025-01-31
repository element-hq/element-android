/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms

import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService

/**
 * An interface for decrypting data.
 */
internal interface IMXDecrypting {

    /**
     * Decrypt an event.
     *
     * @param event the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the decryption information, or an error
     */
    @Throws(MXCryptoError::class)
    suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult

    /**
     * Handle a key event.
     *
     * @param event the key event.
     * @param defaultKeysBackupService the keys backup service
     */
    fun onRoomKeyEvent(event: Event, defaultKeysBackupService: DefaultKeysBackupService, forceAccept: Boolean = false) {}
}
