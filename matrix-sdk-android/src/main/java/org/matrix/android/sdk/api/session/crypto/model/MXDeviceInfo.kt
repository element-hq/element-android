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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class MXDeviceInfo(
        /**
         * The id of this device.
         */
        @Json(name = "device_id")
        val deviceId: String,

        /**
         * the user id
         */
        @Json(name = "user_id")
        val userId: String,

        /**
         * The list of algorithms supported by this device.
         */
        @Json(name = "algorithms")
        val algorithms: List<String>? = null,

        /**
         * A map from "<key type>:<deviceId>" to "<base64-encoded key>".
         */
        @Json(name = "keys")
        val keys: Map<String, String>? = null,

        /**
         * The signature of this MXDeviceInfo.
         * A map from "<userId>" to a map from "<key type>:<deviceId>" to "<signature>"
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>? = null,

        /*
         * Additional data from the homeserver.
         */
        @Json(name = "unsigned")
        val unsigned: JsonDict? = null,

        /**
         * Verification state of this device.
         */
        val verified: Int = DEVICE_VERIFICATION_UNKNOWN
) : Serializable {
    /**
     * Tells if the device is unknown
     *
     * @return true if the device is unknown
     */
    val isUnknown: Boolean
        get() = verified == DEVICE_VERIFICATION_UNKNOWN

    /**
     * Tells if the device is verified.
     *
     * @return true if the device is verified
     */
    val isVerified: Boolean
        get() = verified == DEVICE_VERIFICATION_VERIFIED

    /**
     * Tells if the device is unverified.
     *
     * @return true if the device is unverified
     */
    val isUnverified: Boolean
        get() = verified == DEVICE_VERIFICATION_UNVERIFIED

    /**
     * Tells if the device is blocked.
     *
     * @return true if the device is blocked
     */
    val isBlocked: Boolean
        get() = verified == DEVICE_VERIFICATION_BLOCKED

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
        return unsigned?.get("device_display_name") as? String
    }

    /**
     * @return the signed data map
     */
    fun signalableJSONDictionary(): Map<String, Any> {
        val map = HashMap<String, Any>()

        map["device_id"] = deviceId

        map["user_id"] = userId

        if (null != algorithms) {
            map["algorithms"] = algorithms
        }

        if (null != keys) {
            map["keys"] = keys
        }

        return map
    }

    override fun toString(): String {
        return "MXDeviceInfo $userId:$deviceId"
    }

    companion object {
        // This device is a new device and the user was not warned it has been added.
        const val DEVICE_VERIFICATION_UNKNOWN = -1

        // The user has not yet verified this device.
        const val DEVICE_VERIFICATION_UNVERIFIED = 0

        // The user has verified this device.
        const val DEVICE_VERIFICATION_VERIFIED = 1

        // The user has blocked this device.
        const val DEVICE_VERIFICATION_BLOCKED = 2
    }
}
