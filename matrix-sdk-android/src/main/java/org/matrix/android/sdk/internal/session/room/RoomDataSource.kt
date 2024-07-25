/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
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

        return liveData.map { results ->
            results.firstOrNull().orFalse()
        }
    }
}
