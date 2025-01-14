/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.onboarding.FtueUseCase
import im.vector.app.features.session.VectorSessionStore
import io.mockk.coEvery
import io.mockk.mockk

class FakeVectorStore {
    val instance = mockk<VectorSessionStore>()

    fun givenUseCase(useCase: FtueUseCase?) {
        coEvery {
            instance.readUseCase()
        } coAnswers {
            useCase
        }
    }
}
