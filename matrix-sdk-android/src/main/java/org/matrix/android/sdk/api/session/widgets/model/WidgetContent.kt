/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.widgets.model

import android.annotation.SuppressLint
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.util.safeCapitalize

/**
 * Ref: https://github.com/matrix-org/matrix-doc/issues/1236
 */
@JsonClass(generateAdapter = true)
data class WidgetContent(
        @Json(name = "creatorUserId") val creatorUserId: String? = null,
        @Json(name = "id") val id: String? = null,
        @Json(name = "type") val type: String? = null,
        @Json(name = "url") val url: String? = null,
        @Json(name = "name") val name: String? = null,
        @Json(name = "data") val data: JsonDict = emptyMap(),
        @Json(name = "waitForIframeLoad") val waitForIframeLoad: Boolean = false
) {

    fun isActive() = type != null && url != null

    @SuppressLint("DefaultLocale")
    fun getHumanName(): String {
        return (name ?: type ?: "").safeCapitalize()
    }
}
