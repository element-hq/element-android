/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.Matrix

class FakeMatrix(
        val fakeSecureStorageService: FakeSecureStorageService = FakeSecureStorageService(),
) {

    val instance = mockk<Matrix>()

    init {
        every { instance.secureStorageService() } returns fakeSecureStorageService
    }
}
