/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.query.whereType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.util.fetchCopied
import javax.inject.Inject

/**
 * The crypto module needs some information regarding rooms that are stored
 * in the session DB, this class encapsulate this functionality.
 */
internal class CryptoSessionInfoProvider @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy
) {

    fun isRoomEncrypted(roomId: String): Boolean {
        // We look at the presence at any m.room.encryption state event no matter if it's
        // the latest one or if it is well formed
        val encryptionEvent = monarchy.fetchCopied { realm ->
            EventEntity.whereType(realm, roomId = roomId, type = EventType.STATE_ROOM_ENCRYPTION)
                    .isEmpty(EventEntityFields.STATE_KEY)
                    .findFirst()
        }
        return encryptionEvent != null
    }

    /**
     * @param roomId the room Id
     * @param allActive if true return joined as well as invited, if false, only joined
     */
    fun getRoomUserIds(roomId: String, allActive: Boolean): List<String> {
        var userIds: List<String> = emptyList()
        monarchy.doWithRealm { realm ->
            userIds = if (allActive) {
                RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
            } else {
                RoomMemberHelper(realm, roomId).getJoinedRoomMemberIds()
            }
        }
        return userIds
    }
}
