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
package im.vector.matrix.android.internal.crypto.model

import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.rest.RestDeviceInfo
import im.vector.matrix.android.internal.crypto.store.db.model.CryptoMapper
import im.vector.matrix.android.internal.crypto.store.db.model.DeviceInfoEntity

data class CryptoDeviceInfo(
        val deviceId: String,
        override val userId: String,
        var algorithms: List<String>? = null,
        override val keys: Map<String, String>? = null,
        override val signatures: Map<String, Map<String, String>>? = null,
        val unsigned: JsonDict? = null,
        var trustLevel: DeviceTrustLevel? = null,
        var isBlocked: Boolean = false
) : CryptoInfo {

    val isVerified: Boolean
        get() = trustLevel?.isVerified() ?: false

    val isUnknown: Boolean
        get() = trustLevel == null

    /**
     * @return the fingerprint
     */
    fun fingerprint(): String? {
        return keys
                ?.takeIf { !deviceId.isBlank() }
                ?.get("ed25519:$deviceId")
    }

    /**
     * @return the identity key
     */
    fun identityKey(): String? {
        return keys
                ?.takeIf { !deviceId.isBlank() }
                ?.get("curve25519:$deviceId")
    }

    /**
     * @return the display name
     */
    fun displayName(): String? {
        return unsigned?.get("device_display_name") as? String
    }

    override fun signalableJSONDictionary(): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["device_id"] = deviceId
        map["user_id"] = userId
        algorithms?.let { map["algorithms"] = it }
        keys?.let { map["keys"] = it }
        return map
    }
}

internal fun CryptoDeviceInfo.toRest(): RestDeviceInfo {
    return CryptoInfoMapper.map(this)
}

internal fun CryptoDeviceInfo.toEntity(): DeviceInfoEntity {
    return CryptoMapper.mapToEntity(this)
}
