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
data class CallSelectAnswerContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. Indicates the answer user has chosen.
         */
        @Json(name = "selected_party_id") val selectedPartyId: String? = null,

        /**
         * Required. The version of the VoIP specification this message adheres to.
         */
        @Json(name = "version") override val version: String?
) : CallSignalingContent
