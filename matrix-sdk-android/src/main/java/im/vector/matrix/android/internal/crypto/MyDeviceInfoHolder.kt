/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.session.SessionScope
import java.util.*
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
    val myDevice: MXDeviceInfo = MXDeviceInfo(credentials.deviceId!!, credentials.userId)

    init {
        val keys = HashMap<String, String>()

        if (!TextUtils.isEmpty(olmDevice.deviceEd25519Key)) {
            keys["ed25519:" + credentials.deviceId] = olmDevice.deviceEd25519Key!!
        }

        if (!TextUtils.isEmpty(olmDevice.deviceCurve25519Key)) {
            keys["curve25519:" + credentials.deviceId] = olmDevice.deviceCurve25519Key!!
        }

        myDevice.keys = keys

        myDevice.algorithms = MXCryptoAlgorithms.supportedAlgorithms()
        myDevice.verified = MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED

        // Add our own deviceinfo to the store
        val endToEndDevicesForUser = cryptoStore.getUserDevices(credentials.userId)

        val myDevices: MutableMap<String, MXDeviceInfo>

        if (null != endToEndDevicesForUser) {
            myDevices = HashMap(endToEndDevicesForUser)
        } else {
            myDevices = HashMap()
        }

        myDevices[myDevice.deviceId] = myDevice

        cryptoStore.storeUserDevices(credentials.userId, myDevices)
    }
}