/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api

import org.matrix.android.sdk.api.util.MatrixItem

interface MatrixItemDisplayNameFallbackProvider {
    fun getDefaultName(matrixItem: MatrixItem): String
}
