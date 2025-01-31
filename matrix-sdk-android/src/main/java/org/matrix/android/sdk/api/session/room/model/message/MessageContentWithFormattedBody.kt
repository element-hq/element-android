/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

interface MessageContentWithFormattedBody : MessageContent {
    /**
     * The format used in the formatted_body. Currently only "org.matrix.custom.html" is supported.
     */
    val format: String?

    /**
     * The formatted version of the body. This is required if format is specified.
     */
    val formattedBody: String?

    /**
     * Get the formattedBody, only if not blank and if the format is equal to "org.matrix.custom.html".
     */
    val matrixFormattedBody: String?
        get() = formattedBody?.takeIf { it.isNotBlank() && format == MessageFormat.FORMAT_MATRIX_HTML }
}
