/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.database.model.UserEntity

internal object UserMapper {

    fun map(userEntity: UserEntity): User {
        return User(
                userEntity.userId,
                userEntity.displayName,
                userEntity.avatarUrl
        )
    }
}

internal fun UserEntity.asDomain(): User {
    return UserMapper.map(this)
}
