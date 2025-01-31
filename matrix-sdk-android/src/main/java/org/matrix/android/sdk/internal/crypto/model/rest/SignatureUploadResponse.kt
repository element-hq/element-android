/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Upload Signature response.
 */
@JsonClass(generateAdapter = true)
internal data class SignatureUploadResponse(
        /**
         * The response contains a failures property, which is a map of user ID to device ID to failure reason,
         * if any of the uploaded keys failed.
         * The homeserver should verify that the signatures on the uploaded keys are valid.
         * If a signature is not valid, the homeserver should set the corresponding entry in failures to a JSON object
         * with the errcode property set to M_INVALID_SIGNATURE.
         */
        val failures: Map<String, Map<String, UploadResponseFailure>>? = null
)

@JsonClass(generateAdapter = true)
internal data class UploadResponseFailure(
        @Json(name = "status")
        val status: Int,

        @Json(name = "errcode")
        val errCode: String,

        @Json(name = "message")
        val message: String
)
