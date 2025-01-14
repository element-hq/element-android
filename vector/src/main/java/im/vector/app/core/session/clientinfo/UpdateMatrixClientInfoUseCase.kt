/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import im.vector.app.core.resources.AppNameProvider
import im.vector.app.core.resources.BuildMeta
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

/**
 * This use case updates if needed the account data event containing extended client info.
 */
class UpdateMatrixClientInfoUseCase @Inject constructor(
        private val appNameProvider: AppNameProvider,
        private val buildMeta: BuildMeta,
        private val getMatrixClientInfoUseCase: GetMatrixClientInfoUseCase,
        private val setMatrixClientInfoUseCase: SetMatrixClientInfoUseCase,
) {

    suspend fun execute(session: Session) = runCatching {
        val clientInfo = MatrixClientInfoContent(
                name = appNameProvider.getAppName(),
                version = buildMeta.versionName
        )
        val deviceId = session.sessionParams.deviceId.orEmpty()
        if (deviceId.isNotEmpty()) {
            val storedClientInfo = getMatrixClientInfoUseCase.execute(session, deviceId)
            Timber.d("storedClientInfo=$storedClientInfo, current client info=$clientInfo")
            if (clientInfo != storedClientInfo) {
                Timber.d("client info need to be updated")
                return setMatrixClientInfoUseCase.execute(session, clientInfo)
            }
        } else {
            throw NoDeviceIdError()
        }
    }
}
