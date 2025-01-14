/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import javax.inject.Inject

/**
 * This use case sets the account data event containing extended client info.
 */
class SetMatrixClientInfoUseCase @Inject constructor() {

    suspend fun execute(session: Session, clientInfo: MatrixClientInfoContent): Result<Unit> = runCatching {
        val deviceId = session.sessionParams.deviceId.orEmpty()
        if (deviceId.isNotEmpty()) {
            val type = MATRIX_CLIENT_INFO_KEY_PREFIX + deviceId
            session.accountDataService()
                    .updateUserAccountData(type, clientInfo.toContent())
        } else {
            throw NoDeviceIdError()
        }
    }
}
