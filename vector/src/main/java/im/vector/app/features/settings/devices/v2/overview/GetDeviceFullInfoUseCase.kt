/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import androidx.lifecycle.asFlow
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.v2.GetEncryptionTrustLevelForDeviceUseCase
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.flow.unwrap
import javax.inject.Inject

class GetDeviceFullInfoUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
        private val getEncryptionTrustLevelForDeviceUseCase: GetEncryptionTrustLevelForDeviceUseCase,
        private val checkIfSessionIsInactiveUseCase: CheckIfSessionIsInactiveUseCase,
) {

    fun execute(deviceId: String): Flow<DeviceFullInfo> {
        return activeSessionHolder.getSafeActiveSession()?.let { session ->
            combine(
                    getCurrentSessionCrossSigningInfoUseCase.execute(),
                    session.cryptoService().getMyDevicesInfoLive(deviceId).asFlow(),
                    session.cryptoService().getLiveCryptoDeviceInfoWithId(deviceId).asFlow()
            ) { currentSessionCrossSigningInfo, deviceInfo, cryptoDeviceInfo ->
                val info = deviceInfo.getOrNull()
                val cryptoInfo = cryptoDeviceInfo.getOrNull()
                val fullInfo = if (info != null && cryptoInfo != null) {
                    val roomEncryptionTrustLevel = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoInfo)
                    val isInactive = checkIfSessionIsInactiveUseCase.execute(info.lastSeenTs ?: 0)
                    DeviceFullInfo(
                            deviceInfo = info,
                            cryptoDeviceInfo = cryptoInfo,
                            roomEncryptionTrustLevel = roomEncryptionTrustLevel,
                            isInactive = isInactive
                    )
                } else {
                    null
                }
                fullInfo.toOptional()
            }.unwrap()
        } ?: emptyFlow()
    }
}
