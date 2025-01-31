/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.orFalse

@JsonClass(generateAdapter = true)
internal data class SuccessResult(
        @Json(name = "success")
        val success: Boolean?
) {
    fun isSuccess() = success.orFalse()
}
