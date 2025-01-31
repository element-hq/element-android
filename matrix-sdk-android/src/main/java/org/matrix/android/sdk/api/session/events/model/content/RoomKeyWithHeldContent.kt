/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an sharekey content.
 */
@JsonClass(generateAdapter = true)
data class RoomKeyWithHeldContent(

        /**
         * Required if code is not m.no_olm. The ID of the room that the session belongs to.
         */
        @Json(name = "room_id") val roomId: String? = null,

        /**
         * Required. The encryption algorithm that the key is for.
         */
        @Json(name = "algorithm") val algorithm: String? = null,

        /**
         *  Required if code is not m.no_olm. The ID of the session.
         */
        @Json(name = "session_id") val sessionId: String? = null,

        /**
         * Required. The key of the session creator.
         */
        @Json(name = "sender_key") val senderKey: String? = null,

        /**
         *  Required. A machine-readable code for why the key was not sent
         */
        @Json(name = "code") val codeString: String? = null,

        /**
         *  A human-readable reason for why the key was not sent. The receiving client should only use this string if it does not understand the code.
         */
        @Json(name = "reason") val reason: String? = null,

        /**
         * the device ID of the device sending the m.room_key.withheld message
         * MSC3735.
         */
        @Json(name = "from_device") val fromDevice: String? = null

) {
    val code: WithHeldCode?
        get() {
            return WithHeldCode.fromCode(codeString)
        }
}

enum class WithHeldCode(val value: String) {
    /**
     * the user/device was blacklisted.
     */
    BLACKLISTED("m.blacklisted"),

    /**
     * the user/devices is unverified.
     */
    UNVERIFIED("m.unverified"),

    /**
     * the user/device is not allowed have the key. For example, this would usually be sent in response
     * to a key request if the user was not in the room when the message was sent.
     */
    UNAUTHORISED("m.unauthorised"),

    /**
     * Sent in reply to a key request if the device that the key is requested from does not have the requested key.
     */
    UNAVAILABLE("m.unavailable"),

    /**
     * An olm session could not be established.
     * This may happen, for example, if the sender was unable to obtain a one-time key from the recipient.
     */
    NO_OLM("m.no_olm");

    companion object {
        fun fromCode(code: String?): WithHeldCode? {
            return when (code) {
                BLACKLISTED.value -> BLACKLISTED
                UNVERIFIED.value -> UNVERIFIED
                UNAUTHORISED.value -> UNAUTHORISED
                UNAVAILABLE.value -> UNAVAILABLE
                NO_OLM.value -> NO_OLM
                else -> null
            }
        }
    }
}
