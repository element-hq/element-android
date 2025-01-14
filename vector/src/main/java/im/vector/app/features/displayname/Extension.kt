/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.displayname

import org.matrix.android.sdk.api.util.MatrixItem

fun MatrixItem.getBestName(): String {
    // Note: this code is copied from [DisplayNameResolver] in the SDK
    return if (this is MatrixItem.RoomAliasItem && displayName.isNullOrBlank()) {
        // Best name is the id, and we keep the displayName of the room for the case we need the first letter
        id
    } else {
        displayName
                ?.takeIf { it.isNotBlank() }
                ?: VectorMatrixItemDisplayNameFallbackProvider.getDefaultName(this)
    }
}
