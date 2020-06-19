/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.model

import im.vector.matrix.android.internal.crypto.model.rest.DeviceKeys
import im.vector.matrix.android.internal.crypto.model.rest.DeviceKeysWithUnsigned
import im.vector.matrix.android.internal.crypto.model.rest.RestKeyInfo

internal object CryptoInfoMapper {

    fun map(deviceKeysWithUnsigned: DeviceKeysWithUnsigned): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                deviceId = deviceKeysWithUnsigned.deviceId,
                userId = deviceKeysWithUnsigned.userId,
                algorithms = deviceKeysWithUnsigned.algorithms,
                keys = deviceKeysWithUnsigned.keys,
                signatures = deviceKeysWithUnsigned.signatures,
                unsigned = deviceKeysWithUnsigned.unsigned,
                trustLevel = null
        )
    }

    fun map(cryptoDeviceInfo: CryptoDeviceInfo): DeviceKeys {
        return DeviceKeys(
                deviceId = cryptoDeviceInfo.deviceId,
                algorithms = cryptoDeviceInfo.algorithms,
                keys = cryptoDeviceInfo.keys,
                signatures = cryptoDeviceInfo.signatures,
                userId = cryptoDeviceInfo.userId
        )
    }

    fun map(keyInfo: RestKeyInfo): CryptoCrossSigningKey {
        return CryptoCrossSigningKey(
                userId = keyInfo.userId,
                usages = keyInfo.usages,
                keys = keyInfo.keys.orEmpty(),
                signatures = keyInfo.signatures,
                trustLevel = null
        )
    }

    fun map(keyInfo: CryptoCrossSigningKey): RestKeyInfo {
        return RestKeyInfo(
                userId = keyInfo.userId,
                usages = keyInfo.usages,
                keys = keyInfo.keys,
                signatures = keyInfo.signatures
        )
    }
}
