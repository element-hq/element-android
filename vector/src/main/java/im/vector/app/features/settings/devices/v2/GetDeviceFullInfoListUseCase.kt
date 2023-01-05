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

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.session.clientinfo.GetMatrixClientInfoUseCase
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.filter.FilterDevicesUseCase
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import im.vector.app.features.settings.devices.v2.verification.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.v2.verification.GetEncryptionTrustLevelForDeviceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.flow.flow
import javax.inject.Inject

class GetDeviceFullInfoListUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val checkIfSessionIsInactiveUseCase: CheckIfSessionIsInactiveUseCase,
        private val getEncryptionTrustLevelForDeviceUseCase: GetEncryptionTrustLevelForDeviceUseCase,
        private val getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
        private val filterDevicesUseCase: FilterDevicesUseCase,
        private val parseDeviceUserAgentUseCase: ParseDeviceUserAgentUseCase,
        private val getMatrixClientInfoUseCase: GetMatrixClientInfoUseCase,
) {

    fun execute(filterType: DeviceManagerFilterType, excludeCurrentDevice: Boolean = false): Flow<List<DeviceFullInfo>> {
        return activeSessionHolder.getSafeActiveSession()?.let { session ->
            val deviceFullInfoFlow = combine(
                    getCurrentSessionCrossSigningInfoUseCase.execute(),
                    session.flow().liveUserCryptoDevices(session.myUserId),
                    session.flow().liveMyDevicesInfo()
            ) { currentSessionCrossSigningInfo, cryptoList, infoList ->
                val deviceFullInfoList = convertToDeviceFullInfoList(session, currentSessionCrossSigningInfo, cryptoList, infoList)
                val excludedDeviceIds = if (excludeCurrentDevice) {
                    listOf(currentSessionCrossSigningInfo.deviceId)
                } else {
                    emptyList()
                }
                filterDevicesUseCase.execute(currentSessionCrossSigningInfo, deviceFullInfoList, filterType, excludedDeviceIds)
            }

            deviceFullInfoFlow.distinctUntilChanged()
        } ?: emptyFlow()
    }

    private fun convertToDeviceFullInfoList(
            session: Session,
            currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo,
            cryptoList: List<CryptoDeviceInfo>,
            infoList: List<DeviceInfo>,
    ): List<DeviceFullInfo> {
        return infoList
                .sortedByDescending { it.lastSeenTs }
                .map { deviceInfo ->
                    val cryptoDeviceInfo = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }
                    val roomEncryptionTrustLevel = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)
                    val isInactive = checkIfSessionIsInactiveUseCase.execute(deviceInfo.lastSeenTs)
                    val isCurrentDevice = currentSessionCrossSigningInfo.deviceId == cryptoDeviceInfo?.deviceId
                    val deviceExtendedInfo = parseDeviceUserAgentUseCase.execute(deviceInfo.getBestLastSeenUserAgent())
                    val matrixClientInfo = deviceInfo.deviceId
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { getMatrixClientInfoUseCase.execute(session, it) }

                    DeviceFullInfo(
                            deviceInfo = deviceInfo,
                            cryptoDeviceInfo = cryptoDeviceInfo,
                            roomEncryptionTrustLevel = roomEncryptionTrustLevel,
                            isInactive = isInactive,
                            isCurrentDevice = isCurrentDevice,
                            deviceExtendedInfo = deviceExtendedInfo,
                            matrixClientInfo = matrixClientInfo
                    )
                }
    }
}
