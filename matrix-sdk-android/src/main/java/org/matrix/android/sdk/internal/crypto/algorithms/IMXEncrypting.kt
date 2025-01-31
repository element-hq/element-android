/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.internal.crypto.InboundGroupSessionHolder

/**
 * An interface for encrypting data.
 */
internal interface IMXEncrypting {

    /**
     * Encrypt an event content according to the configuration of the room.
     *
     * @param eventContent the content of the event.
     * @param eventType the type of the event.
     * @param userIds the room members the event will be sent to.
     * @return the encrypted content
     */
    suspend fun encryptEventContent(eventContent: Content, eventType: String, userIds: List<String>): Content

    suspend fun shareHistoryKeysWithDevice(inboundSessionWrapper: InboundGroupSessionHolder, deviceInfo: CryptoDeviceInfo) {}
}
