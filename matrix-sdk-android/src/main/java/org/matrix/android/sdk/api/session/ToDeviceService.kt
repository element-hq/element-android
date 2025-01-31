/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session

import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.events.model.Content
import java.util.UUID

interface ToDeviceService {
    /**
     * Send an event to a specific list of devices.
     */
    suspend fun sendToDevice(eventType: String, contentMap: MXUsersDevicesMap<Any>, txnId: String? = UUID.randomUUID().toString())

    suspend fun sendToDevice(eventType: String, userId: String, deviceId: String, content: Content, txnId: String? = UUID.randomUUID().toString()) {
        sendToDevice(eventType, mapOf(userId to listOf(deviceId)), content, txnId)
    }

    suspend fun sendToDevice(eventType: String, targets: Map<String, List<String>>, content: Content, txnId: String? = UUID.randomUUID().toString())

    suspend fun sendEncryptedToDevice(eventType: String, targets: Map<String, List<String>>, content: Content, txnId: String? = UUID.randomUUID().toString())
}
