/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.displayname

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

internal class DisplayNameResolver @Inject constructor(
        private val matrixConfiguration: MatrixConfiguration
) {
    fun getBestName(matrixItem: MatrixItem): String {
        return if (matrixItem is MatrixItem.RoomAliasItem) {
            // Best name is the id, and we keep the displayName of the room for the case we need the first letter
            matrixItem.id
        } else {
            matrixItem.displayName?.takeIf { it.isNotBlank() }
                    ?: matrixConfiguration.matrixItemDisplayNameFallbackProvider?.getDefaultName(matrixItem)
                    ?: matrixItem.id
        }
    }
}
