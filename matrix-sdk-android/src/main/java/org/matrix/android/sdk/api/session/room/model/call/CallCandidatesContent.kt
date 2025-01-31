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
 * This event is sent by callers after sending an invite and by the callee after answering.
 * Its purpose is to give the other party additional ICE candidates to try using to communicate.
 */
@JsonClass(generateAdapter = true)
data class CallCandidatesContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. Array of objects describing the candidates.
         */
        @Json(name = "candidates") val candidates: List<CallCandidate> = emptyList(),
        /**
         * Required. The version of the VoIP specification this messages adheres to.
         */
        @Json(name = "version") override val version: String?
) : CallSignalingContent
