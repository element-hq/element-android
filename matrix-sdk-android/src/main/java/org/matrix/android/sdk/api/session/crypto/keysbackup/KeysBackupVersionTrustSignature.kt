/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keysbackup

import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

/**
 * A signature in a `KeysBackupVersionTrust` object.
 */

sealed class KeysBackupVersionTrustSignature {

    data class DeviceSignature(
            /**
             * The id of the device that signed the backup version.
             */
            val deviceId: String?,

            /**
             * The device that signed the backup version.
             * Can be null if the device is not known.
             */
            val device: CryptoDeviceInfo?,

            /**
             * Flag to indicate the signature from this device is valid.
             */
            val valid: Boolean
    ) : KeysBackupVersionTrustSignature()

    data class UserSignature(
            val keyId: String?,
            val cryptoCrossSigningKey: CryptoCrossSigningKey?,
            val valid: Boolean
    ) : KeysBackupVersionTrustSignature()
}
