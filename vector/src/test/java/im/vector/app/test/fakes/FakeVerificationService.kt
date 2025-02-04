/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService

class FakeVerificationService : VerificationService by mockk() {

    fun givenEventFlow() {
        every { requestEventFlow() } returns flow { }
    }
}
