/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.UIABaseAuth

@JsonClass(generateAdapter = true)
internal data class DeactivateAccountParams(
        // Set to true to erase all data of the account
        @Json(name = "erase")
        val erase: Boolean,

        @Json(name = "auth")
        val auth: Map<String, *>? = null
) {
    companion object {
        fun create(auth: UIABaseAuth?, erase: Boolean): DeactivateAccountParams {
            return DeactivateAccountParams(
                    auth = auth?.asMap(),
                    erase = erase
            )
        }
    }
}
