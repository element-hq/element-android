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
     * my device info
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
