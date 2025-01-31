/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.accountdata

import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.getDirectRooms
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.sync.model.accountdata.DirectMessagesContent
import javax.inject.Inject

internal class DirectChatsHelper @Inject constructor(
        @SessionDatabase private val realmConfiguration: RealmConfiguration
) {

    /**
     * @return a map of userId <-> list of roomId
     */
    fun getLocalDirectMessages(filterRoomId: String? = null): DirectMessagesContent {
        return Realm.getInstance(realmConfiguration).use { realm ->
            // Makes sure we have the latest realm updates, this is important as we sent this information to the server.
            realm.refresh()
            RoomSummaryEntity.getDirectRooms(realm)
                    .asSequence()
                    .filter { it.roomId != filterRoomId && it.directUserId != null && it.membership.isActive() }
                    .groupByTo(mutableMapOf(), { it.directUserId!! }, { it.roomId })
        }
    }
}
