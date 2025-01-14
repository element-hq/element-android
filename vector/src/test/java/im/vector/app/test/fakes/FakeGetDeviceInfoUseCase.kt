/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.device.GetDeviceInfoUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

class FakeGetDeviceInfoUseCase : GetDeviceInfoUseCase by mockk() {

    fun givenDeviceInfo(cryptoDeviceInfo: CryptoDeviceInfo) {
        coEvery { execute() } returns cryptoDeviceInfo
    }
}
