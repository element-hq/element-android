/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import androidx.lifecycle.asFlow
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.session.clientinfo.GetMatrixClientInfoUseCase
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.ParseDeviceUserAgentUseCase
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import im.vector.app.features.settings.devices.v2.verification.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.v2.verification.GetEncryptionTrustLevelForDeviceUseCase
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
        private val parseDeviceUserAgentUseCase: ParseDeviceUserAgentUseCase,
        private val getMatrixClientInfoUseCase: GetMatrixClientInfoUseCase,
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
                val fullInfo = if (info != null) {
                    val roomEncryptionTrustLevel = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoInfo)
                    val isInactive = checkIfSessionIsInactiveUseCase.execute(info.lastSeenTs)
                    val isCurrentDevice = currentSessionCrossSigningInfo.deviceId == info.deviceId
                    val deviceUserAgent = parseDeviceUserAgentUseCase.execute(info.getBestLastSeenUserAgent())
                    val matrixClientInfo = info.deviceId
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { getMatrixClientInfoUseCase.execute(session, it) }

                    DeviceFullInfo(
                            deviceInfo = info,
                            cryptoDeviceInfo = cryptoInfo,
                            roomEncryptionTrustLevel = roomEncryptionTrustLevel,
                            isInactive = isInactive,
                            isCurrentDevice = isCurrentDevice,
                            deviceExtendedInfo = deviceUserAgent,
                            matrixClientInfo = matrixClientInfo
                    )
                } else {
                    null
                }
                fullInfo.toOptional()
            }.unwrap()
        } ?: emptyFlow()
    }
}
