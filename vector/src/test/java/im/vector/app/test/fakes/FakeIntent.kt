/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk

class FakeIntent {

    val instance = mockk<Intent>()

    fun givenResolvesType(context: Context, type: String?) {
        every { instance.resolveType(context) } returns type
    }

    fun givenCharSequenceExtra(key: String, value: CharSequence) {
        every { instance.getCharSequenceExtra(key) } returns value
    }
}
