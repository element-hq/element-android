/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import org.amshove.kluent.shouldBeEqualTo

class FakeFunction1<T : Any> {

    private lateinit var capturedValue: T

    val capture: (T) -> Unit = {
        capturedValue = it
    }

    fun verifyNoInteractions() {
        this::capturedValue.isInitialized shouldBeEqualTo false
    }

    fun assertValue(value: T) {
        capturedValue shouldBeEqualTo value
    }
}
