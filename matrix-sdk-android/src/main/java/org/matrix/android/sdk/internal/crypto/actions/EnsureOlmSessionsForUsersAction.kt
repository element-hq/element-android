/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.actions

import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.model.MXOlmSessionResult
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import timber.log.Timber
import javax.inject.Inject

internal class EnsureOlmSessionsForUsersAction @Inject constructor(
        private val olmDevice: MXOlmDevice,
        private val cryptoStore: IMXCryptoStore,
        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction
) {

    /**
     * Try to make sure we have established olm sessions for the given users.
     * @param users a list of user ids.
     */
    suspend fun handle(users: List<String>): MXUsersDevicesMap<MXOlmSessionResult> {
        Timber.v("## ensureOlmSessionsForUsers() : ensureOlmSessionsForUsers $users")
        val devicesByUser = users.associateWith { userId ->
            val devices = cryptoStore.getUserDevices(userId)?.values.orEmpty()

            devices.filter {
                // Don't bother setting up session to ourself
                it.identityKey() != olmDevice.deviceCurve25519Key &&
                        // Don't bother setting up sessions with blocked users
                        !(it.trustLevel?.isVerified() ?: false)
            }
        }
        return ensureOlmSessionsForDevicesAction.handle(devicesByUser)
    }
}
