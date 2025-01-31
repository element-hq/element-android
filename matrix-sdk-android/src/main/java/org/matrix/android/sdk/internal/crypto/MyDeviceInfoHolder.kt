/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class MyDeviceInfoHolder @Inject constructor(
        // The credentials,
        credentials: Credentials,
        // the crypto store
        cryptoStore: IMXCryptoStore,
        // Olm device
        olmDevice: MXOlmDevice
) {
    // Our device keys
    /**
     * my device info.
     */
    val myDevice: CryptoDeviceInfo

    init {

        val keys = HashMap<String, String>()

// TODO it's a bit strange, why not load from DB?
        if (!olmDevice.deviceEd25519Key.isNullOrEmpty()) {
            keys["ed25519:" + credentials.deviceId] = olmDevice.deviceEd25519Key!!
        }

        if (!olmDevice.deviceCurve25519Key.isNullOrEmpty()) {
            keys["curve25519:" + credentials.deviceId] = olmDevice.deviceCurve25519Key!!
        }

//        myDevice.keys = keys
//
//        myDevice.algorithms = MXCryptoAlgorithms.supportedAlgorithms()

        // TODO hwo to really check cross signed status?
        //
        val crossSigned = cryptoStore.getMyCrossSigningInfo()?.masterKey()?.trustLevel?.locallyVerified ?: false
//        myDevice.trustLevel = DeviceTrustLevel(crossSigned, true)

        myDevice = CryptoDeviceInfo(
                credentials.deviceId!!,
                credentials.userId,
                keys = keys,
                algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
                trustLevel = DeviceTrustLevel(crossSigned, true)
        )

        // Add our own deviceinfo to the store
        val endToEndDevicesForUser = cryptoStore.getUserDevices(credentials.userId)

        val myDevices = endToEndDevicesForUser.orEmpty().toMutableMap()

        myDevices[myDevice.deviceId] = myDevice

        cryptoStore.storeUserDevices(credentials.userId, myDevices)
    }
}
