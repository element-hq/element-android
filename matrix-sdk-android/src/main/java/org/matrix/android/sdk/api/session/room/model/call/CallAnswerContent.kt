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
 * This event is sent by the callee when they wish to answer the call.
 */
@JsonClass(generateAdapter = true)
data class CallAnswerContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. The session description object
         */
        @Json(name = "answer") val answer: Answer,
        /**
         * Required. The version of the VoIP specification this messages adheres to.
         */
        @Json(name = "version") override val version: String?,
        /**
         * Capability advertisement.
         */
        @Json(name = "capabilities") val capabilities: CallCapabilities? = null
) : CallSignalingContent {

    @JsonClass(generateAdapter = true)
    data class Answer(
            /**
             * Required. The type of session description. Must be 'answer'.
             */
            @Json(name = "type") val type: SdpType = SdpType.ANSWER,
            /**
             * Required. The SDP text of the session description.
             */
            @Json(name = "sdp") val sdp: String
    )
}
