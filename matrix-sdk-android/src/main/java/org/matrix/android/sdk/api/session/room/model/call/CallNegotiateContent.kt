/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This introduces SDP negotiation semantics for media pause, hold/resume, ICE restarts and voice/video call up/downgrading.
 */
@JsonClass(generateAdapter = true)
data class CallNegotiateContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. The time in milliseconds that the negotiation is valid for. Once exceeded the sender
         * of the negotiate event should consider the negotiation failed (timed out) and the recipient should ignore it.
         **/
        @Json(name = "lifetime") val lifetime: Int?,
        /**
         * Required. The session description object
         */
        @Json(name = "description") val description: Description? = null,

        /**
         * Required. The version of the VoIP specification this message adheres to.
         */
        @Json(name = "version") override val version: String?

) : CallSignalingContent {
    @JsonClass(generateAdapter = true)
    data class Description(
            /**
             * Required. The type of session description.
             */
            @Json(name = "type") val type: SdpType?,
            /**
             * Required. The SDP text of the session description.
             */
            @Json(name = "sdp") val sdp: String?
    )
}
