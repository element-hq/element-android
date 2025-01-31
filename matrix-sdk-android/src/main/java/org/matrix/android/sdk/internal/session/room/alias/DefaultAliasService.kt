/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.alias

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.room.alias.AliasService

internal class DefaultAliasService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val getRoomLocalAliasesTask: GetRoomLocalAliasesTask,
        private val addRoomAliasTask: AddRoomAliasTask
) : AliasService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultAliasService
    }

    override suspend fun getRoomAliases(): List<String> {
        return getRoomLocalAliasesTask.execute(GetRoomLocalAliasesTask.Params(roomId))
    }

    override suspend fun addAlias(aliasLocalPart: String) {
        addRoomAliasTask.execute(AddRoomAliasTask.Params(roomId, aliasLocalPart))
    }
}
