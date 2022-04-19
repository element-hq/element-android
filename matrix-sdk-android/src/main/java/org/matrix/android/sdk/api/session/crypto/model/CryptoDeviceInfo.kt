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

package org.matrix.android.sdk.api.session.crypto.model

import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.CryptoInfo

data class CryptoDeviceInfo(
        val deviceId: String,
        override val userId: String,
        val algorithms: List<String>? = null,
        override val keys: Map<String, String>? = null,
        override val signatures: Map<String, Map<String, String>>? = null,
        val unsigned: UnsignedDeviceInfo? = null,
        var trustLevel: DeviceTrustLevel? = null,
        val isBlocked: Boolean = false,
        val firstTimeSeenLocalTs: Long? = null
) : CryptoInfo {

    val isVerified: Boolean
        get() = trustLevel?.isVerified() == true

    val isUnknown: Boolean
        get() = trustLevel == null

    /**
     * @return the fingerprint
     */
    fun fingerprint(): String? {
        return keys
                ?.takeIf { deviceId.isNotBlank() }
                ?.get("ed25519:$deviceId")
    }

    /**
     * @return the identity key
     */
    fun identityKey(): String? {
        return keys
                ?.takeIf { deviceId.isNotBlank() }
                ?.get("curve25519:$deviceId")
    }

    /**
     * @return the display name
     */
    fun displayName(): String? {
        return unsigned?.deviceDisplayName
    }

    override fun signalableJSONDictionary(): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["device_id"] = deviceId
        map["user_id"] = userId
        algorithms?.let { map["algorithms"] = it }
        keys?.let { map["keys"] = it }
        return map
    }

    fun shortDebugString() = "$userId|$deviceId"
}
