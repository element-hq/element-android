/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.send

import org.matrix.android.sdk.api.util.MatrixItem

/**
 * Tag class for spans that should mention a matrix item.
 * These Spans will be transformed into pills when detected in message to send
 */
interface MatrixItemSpan {
    val matrixItem: MatrixItem
}
