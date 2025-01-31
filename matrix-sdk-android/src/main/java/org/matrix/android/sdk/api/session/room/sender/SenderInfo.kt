/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.sender

import org.matrix.android.sdk.internal.util.replaceSpaceChars

data class SenderInfo(
        val userId: String,
        /**
         * Consider using [disambiguatedDisplayName].
         */
        val displayName: String?,
        val isUniqueDisplayName: Boolean,
        val avatarUrl: String?
) {
    val disambiguatedDisplayName: String
        get() = when {
            displayName == null -> userId
            displayName.replaceSpaceChars().isBlank() -> "$displayName ($userId)"
            isUniqueDisplayName -> displayName
            else -> "$displayName ($userId)"
        }
}
