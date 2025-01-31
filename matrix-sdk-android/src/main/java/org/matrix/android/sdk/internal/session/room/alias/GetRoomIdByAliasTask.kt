/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.alias

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.alias.RoomAliasDescription
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.findByAlias
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.directory.DirectoryAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetRoomIdByAliasTask : Task<GetRoomIdByAliasTask.Params, Optional<RoomAliasDescription>> {
    data class Params(
            val roomAlias: String,
            val searchOnServer: Boolean
    )
}

internal class DefaultGetRoomIdByAliasTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val directoryAPI: DirectoryAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetRoomIdByAliasTask {

    override suspend fun execute(params: GetRoomIdByAliasTask.Params): Optional<RoomAliasDescription> {
        val roomId = Realm.getInstance(monarchy.realmConfiguration).use {
            RoomSummaryEntity.findByAlias(it, params.roomAlias)?.roomId
        }
        return if (roomId != null) {
            Optional.from(RoomAliasDescription(roomId))
        } else if (!params.searchOnServer) {
            Optional.from(null)
        } else {
            val description = tryOrNull("## Failed to get roomId from alias") {
                executeRequest(globalErrorReceiver) {
                    directoryAPI.getRoomIdByAlias(params.roomAlias)
                }
            }
            Optional.from(description)
        }
    }
}
