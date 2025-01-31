/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.alias

import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.directory.DirectoryAPI
import org.matrix.android.sdk.internal.session.room.alias.RoomAliasAvailabilityChecker.Companion.toFullLocalAlias
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface AddRoomAliasTask : Task<AddRoomAliasTask.Params, Unit> {
    data class Params(
            val roomId: String,
            /**
             * the local part of the alias.
             * Ex: for the alias "#my_alias:example.org", the local part is "my_alias"
             */
            val aliasLocalPart: String
    )
}

internal class DefaultAddRoomAliasTask @Inject constructor(
        @UserId private val userId: String,
        private val directoryAPI: DirectoryAPI,
        private val aliasAvailabilityChecker: RoomAliasAvailabilityChecker,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AddRoomAliasTask {

    override suspend fun execute(params: AddRoomAliasTask.Params) {
        aliasAvailabilityChecker.check(params.aliasLocalPart)

        executeRequest(globalErrorReceiver) {
            directoryAPI.addRoomAlias(
                    roomAlias = params.aliasLocalPart.toFullLocalAlias(userId),
                    body = AddRoomAliasBody(
                            roomId = params.roomId
                    )
            )
        }
    }
}
