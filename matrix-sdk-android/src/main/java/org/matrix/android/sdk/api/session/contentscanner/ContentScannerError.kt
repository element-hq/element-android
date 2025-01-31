/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.contentscanner

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentScannerError(
        @Json(name = "info") val info: String? = null,
        @Json(name = "reason") val reason: String? = null
) {
    companion object {
        // 502 The server failed to request media from the media repo.
        const val REASON_MCS_MEDIA_REQUEST_FAILED = "MCS_MEDIA_REQUEST_FAILED"

        /* 400 The server failed to decrypt the encrypted media downloaded from the media repo.*/
        const val REASON_MCS_MEDIA_FAILED_TO_DECRYPT = "MCS_MEDIA_FAILED_TO_DECRYPT"

        /* 403 The server scanned the downloaded media but the antivirus script returned a non-zero exit code.*/
        const val REASON_MCS_MEDIA_NOT_CLEAN = "MCS_MEDIA_NOT_CLEAN"

        /* 403 The provided encrypted_body could not be decrypted. The client should request the public key of the server and then retry (once).*/
        const val REASON_MCS_BAD_DECRYPTION = "MCS_BAD_DECRYPTION"

        /* 400 The request body contains malformed JSON.*/
        const val REASON_MCS_MALFORMED_JSON = "MCS_MALFORMED_JSON"
    }
}

class ScanFailure(val error: ContentScannerError, val httpCode: Int, cause: Throwable? = null) : Throwable(cause = cause)

// For Glide, which deals with Exception and not with Throwable
fun ScanFailure.toException() = Exception(this)
fun Throwable.toScanFailure() = this.cause as? ScanFailure
