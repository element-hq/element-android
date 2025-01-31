/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.MutableLiveData
import io.mockk.mockk
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.util.Optional

class FakeCryptoService(
        val fakeCrossSigningService: FakeCrossSigningService = FakeCrossSigningService(),
        val fakeVerificationService: FakeVerificationService = FakeVerificationService(),
) : CryptoService by mockk() {

    var roomKeysExport = ByteArray(size = 1)
    var cryptoDeviceInfos = mutableMapOf<String, CryptoDeviceInfo>()
    var cryptoDeviceInfoWithIdLiveData: MutableLiveData<Optional<CryptoDeviceInfo>> = MutableLiveData()
    var myDevicesInfoWithIdLiveData: MutableLiveData<Optional<DeviceInfo>> = MutableLiveData()

    override fun crossSigningService() = fakeCrossSigningService

    override fun verificationService() = fakeVerificationService

    override suspend fun exportRoomKeys(password: String) = roomKeysExport

    override fun getLiveCryptoDeviceInfo() = MutableLiveData(cryptoDeviceInfos.values.toList())

    override fun getLiveCryptoDeviceInfo(userId: String) = getLiveCryptoDeviceInfo(listOf(userId))

    override fun getLiveCryptoDeviceInfo(userIds: List<String>) = MutableLiveData(
            cryptoDeviceInfos.filterKeys { userIds.contains(it) }.values.toList()
    )

    override fun getLiveCryptoDeviceInfoWithId(deviceId: String) = cryptoDeviceInfoWithIdLiveData

    override fun getMyDevicesInfoLive(deviceId: String) = myDevicesInfoWithIdLiveData
}
