/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms.olm

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForUsersAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.algorithms.IMXEncrypting
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore

internal class MXOlmEncryption(
        private val roomId: String,
        private val olmDevice: MXOlmDevice,
        private val cryptoStore: IMXCryptoStore,
        private val messageEncrypter: MessageEncrypter,
        private val deviceListManager: DeviceListManager,
        private val ensureOlmSessionsForUsersAction: EnsureOlmSessionsForUsersAction
) :
        IMXEncrypting {

    override suspend fun encryptEventContent(eventContent: Content, eventType: String, userIds: List<String>): Content {
        // pick the list of recipients based on the membership list.
        //
        // TODO there is a race condition here! What if a new user turns up
        ensureSession(userIds)
        val deviceInfos = ArrayList<CryptoDeviceInfo>()
        for (userId in userIds) {
            val devices = cryptoStore.getUserDevices(userId)?.values.orEmpty()
            for (device in devices) {
                val key = device.identityKey()
                if (key == olmDevice.deviceCurve25519Key) {
                    // Don't bother setting up session to ourself
                    continue
                }
                if (device.isBlocked) {
                    // Don't bother setting up sessions with blocked users
                    continue
                }
                deviceInfos.add(device)
            }
        }

        val messageMap = mapOf(
                "room_id" to roomId,
                "type" to eventType,
                "content" to eventContent
        )

        messageEncrypter.encryptMessage(messageMap, deviceInfos)
        return messageMap.toContent()
    }

    /**
     * Ensure that the session.
     *
     * @param users the user ids list
     */
    private suspend fun ensureSession(users: List<String>) {
        deviceListManager.downloadKeys(users, false)
        ensureOlmSessionsForUsersAction.handle(users)
    }
}
