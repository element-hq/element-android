/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.uploads

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.room.uploads.GetUploadsResult
import org.matrix.android.sdk.api.session.room.uploads.UploadsService

internal class DefaultUploadsService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val getUploadsTask: GetUploadsTask,
        private val cryptoService: CryptoService
) : UploadsService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultUploadsService
    }

    override suspend fun getUploads(numberOfEvents: Int, since: String?): GetUploadsResult {
        return getUploadsTask.execute(GetUploadsTask.Params(roomId, cryptoService.isRoomEncrypted(roomId), numberOfEvents, since))
    }
}
