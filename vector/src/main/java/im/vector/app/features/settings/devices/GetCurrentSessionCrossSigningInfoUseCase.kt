/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import javax.inject.Inject

class GetCurrentSessionCrossSigningInfoUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    suspend fun execute(): CurrentSessionCrossSigningInfo {
        val session = activeSessionHolder.getActiveSession()
        val isCrossSigningInitialized = session.cryptoService().crossSigningService().isCrossSigningInitialized()
        val isCrossSigningVerified = session.cryptoService().crossSigningService().isCrossSigningVerified()
        return CurrentSessionCrossSigningInfo(
                deviceId = session.sessionParams.deviceId,
                isCrossSigningInitialized = isCrossSigningInitialized,
                isCrossSigningVerified = isCrossSigningVerified
        )
    }
}
