/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity

internal open class RoomMemberSummaryEntity(
        @PrimaryKey var primaryKey: String = "",
        @Index var userId: String = "",
        @Index var roomId: String = "",
        @Index var displayName: String? = null,
        var avatarUrl: String? = null,
        var reason: String? = null,
        var isDirect: Boolean = false
) : RealmObject() {

    private var membershipStr: String = Membership.NONE.name
    var membership: Membership
        get() {
            return Membership.valueOf(membershipStr)
        }
        set(value) {
            membershipStr = value.name
        }

    var userPresenceEntity: UserPresenceEntity? = null
        set(value) {
            if (value != field) field = value
        }

    fun toMatrixItem() = MatrixItem.UserItem(userId, displayName, avatarUrl)

    companion object
}
