/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

/**
 * Contains a text and eventually a formatted text.
 */
data class TextContent(
        val text: String,
        val formattedText: String? = null
) {
    fun takeFormatted() = formattedText ?: text
}
