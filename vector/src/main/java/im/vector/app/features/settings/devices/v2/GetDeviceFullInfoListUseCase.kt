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
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.flow.flow
import javax.inject.Inject

class GetDeviceFullInfoListUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val checkIfSessionIsInactiveUseCase: CheckIfSessionIsInactiveUseCase,
        private val getEncryptionTrustLevelForDeviceUseCase: GetEncryptionTrustLevelForDeviceUseCase,
        private val getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
) {

    fun execute(): Flow<List<DeviceFullInfo>> {
        return activeSessionHolder.getSafeActiveSession()?.let { session ->
            val deviceFullInfoFlow = combine(
                    getCurrentSessionCrossSigningInfoUseCase.execute(),
                    session.flow().liveUserCryptoDevices(session.myUserId),
                    session.flow().liveMyDevicesInfo()
            ) { currentSessionCrossSigningInfo, cryptoList, infoList ->
                convertToDeviceFullInfoList(currentSessionCrossSigningInfo, cryptoList, infoList)
            }

            deviceFullInfoFlow.distinctUntilChanged()
        } ?: emptyFlow()
    }

    private fun convertToDeviceFullInfoList(
            currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo,
            cryptoList: List<CryptoDeviceInfo>,
            infoList: List<DeviceInfo>,
    ): List<DeviceFullInfo> {
        return infoList
                .sortedByDescending { it.lastSeenTs }
                .map { deviceInfo ->
                    val cryptoDeviceInfo = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }
                    val roomEncryptionTrustLevel = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)
                    val isInactive = checkIfSessionIsInactiveUseCase.execute(deviceInfo.lastSeenTs ?: 0)
                    DeviceFullInfo(deviceInfo, cryptoDeviceInfo, roomEncryptionTrustLevel, isInactive)
                }
    }
}
