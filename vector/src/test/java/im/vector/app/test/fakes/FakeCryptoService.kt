/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.test.fakes

import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.matrix.android.sdk.api.MatrixCallback
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

    fun givenSetDeviceNameSucceeds() {
        val matrixCallback = slot<MatrixCallback<Unit>>()
        every { setDeviceName(any(), any(), capture(matrixCallback)) } answers {
            thirdArg<MatrixCallback<Unit>>().onSuccess(Unit)
        }
    }

    fun givenSetDeviceNameFailsWithError(error: Exception) {
        val matrixCallback = slot<MatrixCallback<Unit>>()
        every { setDeviceName(any(), any(), capture(matrixCallback)) } answers {
            thirdArg<MatrixCallback<Unit>>().onFailure(error)
        }
    }
}
