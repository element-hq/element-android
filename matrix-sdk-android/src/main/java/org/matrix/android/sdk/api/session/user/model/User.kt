/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.user.model

import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.util.JsonDict

/**
 * Data class which holds information about a user.
 * It can be retrieved with [org.matrix.android.sdk.api.session.user.UserService]
 */
data class User(
        val userId: String,
        /**
         * For usage in UI, consider converting to MatrixItem and call getBestName().
         */
        val displayName: String? = null,
        val avatarUrl: String? = null
) {

    companion object {

        fun fromJson(userId: String, json: JsonDict) = User(
                userId = userId,
                displayName = json[ProfileService.DISPLAY_NAME_KEY] as? String,
                avatarUrl = json[ProfileService.AVATAR_URL_KEY] as? String
        )
    }
}
