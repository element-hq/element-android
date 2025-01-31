/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
