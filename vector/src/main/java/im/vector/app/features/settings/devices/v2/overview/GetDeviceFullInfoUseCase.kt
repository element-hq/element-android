/*
 * Copyright (c) 2022 New Vector Ltd
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
