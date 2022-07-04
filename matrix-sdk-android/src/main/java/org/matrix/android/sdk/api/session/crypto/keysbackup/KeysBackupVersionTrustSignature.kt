/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
