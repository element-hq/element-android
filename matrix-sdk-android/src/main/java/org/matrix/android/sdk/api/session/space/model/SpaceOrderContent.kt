/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space.model

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.MatrixPatterns

/**
 * Example:
 * <pre>
 * {
 * "type": "m.space_order",
 *   "content": {
 *       "order": "..."
 *   }
 * }
 * </pre>.
 */
@JsonClass(generateAdapter = true)
data class SpaceOrderContent(
        val order: String? = null
) {
    fun safeOrder(): String? {
        return order?.takeIf { MatrixPatterns.isValidOrderString(it) }
    }
}
