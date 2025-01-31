/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.filter.FilterDevicesUseCase
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
        private val filterDevicesUseCase: FilterDevicesUseCase,
) {

    fun execute(filterType: DeviceManagerFilterType, excludeCurrentDevice: Boolean = false): Flow<List<DeviceFullInfo>> {
        return activeSessionHolder.getSafeActiveSession()?.let { session ->
            val deviceFullInfoFlow = combine(
                    getCurrentSessionCrossSigningInfoUseCase.execute(),
                    session.flow().liveUserCryptoDevices(session.myUserId),
                    session.flow().liveMyDevicesInfo()
            ) { currentSessionCrossSigningInfo, cryptoList, infoList ->
                val deviceFullInfoList = convertToDeviceFullInfoList(currentSessionCrossSigningInfo, cryptoList, infoList)
                val excludedDeviceIds = if (excludeCurrentDevice) {
                    listOf(currentSessionCrossSigningInfo.deviceId)
                } else {
                    emptyList()
                }
                filterDevicesUseCase.execute(deviceFullInfoList, filterType, excludedDeviceIds)
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
