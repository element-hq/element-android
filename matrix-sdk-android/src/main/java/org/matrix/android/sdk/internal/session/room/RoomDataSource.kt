/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMembersLoadStatusType
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class RoomDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) {
    fun getRoomMembersLoadStatus(roomId: String): RoomMembersLoadStatusType {
        var result: RoomMembersLoadStatusType?
        Realm.getInstance(monarchy.realmConfiguration).use {
            result = RoomEntity.where(it, roomId).findFirst()?.membersLoadStatus
        }
        return result ?: RoomMembersLoadStatusType.NONE
    }

    fun getRoomMembersLoadStatusLive(roomId: String): LiveData<Boolean> {
        val liveData = monarchy.findAllMappedWithChanges(
                {
                    RoomEntity.where(it, roomId)
                },
                {
                    it.membersLoadStatus == RoomMembersLoadStatusType.LOADED
                }
        )

        return Transformations.map(liveData) { results ->
            results.firstOrNull().orFalse()
        }
    }
}
