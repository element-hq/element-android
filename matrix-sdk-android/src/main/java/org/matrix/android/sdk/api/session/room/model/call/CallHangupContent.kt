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
 * Sent by either party to signal their termination of the call. This can be sent either once
 * the call has been established or before to abort the call.
 */
@JsonClass(generateAdapter = true)
data class CallHangupContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. The version of the VoIP specification this message adheres to.
         */
        @Json(name = "version") override val version: String?,
        /**
         * Optional error reason for the hangup. This should not be provided when the user naturally ends or rejects the call.
         * When there was an error in the call negotiation, this should be `ice_failed` for when ICE negotiation fails
         * or `invite_timeout` for when the other party did not answer in time.
         * One of: ["ice_failed", "invite_timeout"]
         */
        @Json(name = "reason") val reason: EndCallReason? = null
) : CallSignalingContent
