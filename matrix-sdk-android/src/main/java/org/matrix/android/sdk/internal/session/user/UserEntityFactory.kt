/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user

import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.database.model.UserEntity

internal object UserEntityFactory {

    fun create(userId: String, roomMember: RoomMemberContent): UserEntity {
        return UserEntity(
                userId = userId,
                displayName = roomMember.displayName.orEmpty(),
                avatarUrl = roomMember.avatarUrl.orEmpty()
        )
    }

    fun create(user: User): UserEntity {
        return UserEntity(
                userId = user.userId,
                displayName = user.displayName.orEmpty(),
                avatarUrl = user.avatarUrl.orEmpty()
        )
    }
}
