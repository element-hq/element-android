/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.app.test.fixtures.CryptoDeviceInfoFixture.aCryptoDeviceInfo
import io.mockk.coEvery
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
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
    var cryptoDeviceInfo = aCryptoDeviceInfo()

    override fun crossSigningService() = fakeCrossSigningService

    override fun verificationService() = fakeVerificationService

    override suspend fun exportRoomKeys(password: String) = roomKeysExport

    override fun getLiveCryptoDeviceInfo() = MutableLiveData(cryptoDeviceInfos.values.toList())

    override fun getLiveCryptoDeviceInfo(userId: String): LiveData<List<CryptoDeviceInfo>> {
        return MutableLiveData(
                cryptoDeviceInfos.filterKeys { it == userId }.values.toList()
        )
    }

    override fun getLiveCryptoDeviceInfoWithId(deviceId: String) = cryptoDeviceInfoWithIdLiveData

    override fun getMyDevicesInfoLive(deviceId: String) = myDevicesInfoWithIdLiveData

    fun givenSetDeviceNameSucceeds() {
        coEvery { setDeviceName(any(), any()) } answers {
            Unit
        }
    }

    fun givenSetDeviceNameFailsWithError(error: Exception) {
        coEvery { setDeviceName(any(), any()) } answers {
            throw error
        }
    }

    fun givenDeleteDevicesSucceeds(deviceIds: List<String>) {
        coEvery { deleteDevices(deviceIds, any()) } returns Unit
    }

    fun givenDeleteDevicesNeedsUIAuth(deviceIds: List<String>) {
        coEvery { deleteDevices(deviceIds, any()) } answers {
            secondArg<UserInteractiveAuthInterceptor>().performStage(mockk(), "", mockk())
        }
    }

    fun givenDeleteDevicesFailsWithError(deviceIds: List<String>, error: Exception) {
        coEvery { deleteDevices(deviceIds, any()) } answers {
            throw error
        }
    }

    override suspend fun getMyCryptoDevice() = cryptoDeviceInfo
}
