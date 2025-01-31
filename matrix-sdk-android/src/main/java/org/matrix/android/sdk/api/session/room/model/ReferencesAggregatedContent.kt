/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.verification.VerificationState

/**
 * Contains an aggregated summary info of the references.
 * Put pre-computed info that you want to access quickly without having
 * to go through all references events
 */
@JsonClass(generateAdapter = true)
data class ReferencesAggregatedContent(
        // Verification status info for m.key.verification.request msgType events
        @Json(name = "verif_sum") val verificationState: VerificationState
        // Add more fields for future summary info.
)
