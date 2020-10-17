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

package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Upload Signature response
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
