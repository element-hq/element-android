/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetMediaConfigResult(
        /**
         * The maximum size an upload can be in bytes. Clients SHOULD use this as a guide when uploading content.
         * If not listed or null, the size limit should be treated as unknown.
         */
        @Json(name = "m.upload.size")
        val maxUploadSize: Long? = null
)
