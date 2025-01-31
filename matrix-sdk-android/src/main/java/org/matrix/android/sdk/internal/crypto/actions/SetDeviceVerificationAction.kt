/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.actions

import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.di.UserId
import timber.log.Timber
import javax.inject.Inject

internal class SetDeviceVerificationAction @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
        @UserId private val userId: String,
        private val defaultKeysBackupService: DefaultKeysBackupService
) {

    fun handle(trustLevel: DeviceTrustLevel, userId: String, deviceId: String) {
        val device = cryptoStore.getUserDevice(userId, deviceId)

        // Sanity check
        if (null == device) {
            Timber.w("## setDeviceVerification() : Unknown device $userId:$deviceId")
            return
        }

        if (device.isVerified != trustLevel.isVerified()) {
            if (userId == this.userId) {
                // If one of the user's own devices is being marked as verified / unverified,
                // check the key backup status, since whether or not we use this depends on
                // whether it has a signature from a verified device
                defaultKeysBackupService.checkAndStartKeysBackup()
            }
        }

        if (device.trustLevel != trustLevel) {
            device.trustLevel = trustLevel
            cryptoStore.setDeviceTrust(userId, deviceId, trustLevel.crossSigningVerified, trustLevel.locallyVerified)
        }
    }
}
