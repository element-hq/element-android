/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * {
 *      "clean": true,
 *      "info": "File clean at 6/7/2018, 6:02:40 PM"
 *  }
 */
@JsonClass(generateAdapter = true)
data class ScanResponse(
        @Json(name = "clean") val clean: Boolean,
        /** Human-readable information about the result. */
        @Json(name = "info") val info: String?
)
