/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.resources.StringProvider
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.every
import io.mockk.mockk

class FakeStringProvider {
    val instance = mockk<StringProvider>()

    init {
        every { instance.getString(any()) } answers {
            "test-${args[0]}"
        }
        every { instance.getString(any(), any()) } answers {
            "test-${args[0]}-${args[1].toStr()}"
        }

        every { instance.getQuantityString(any(), any(), any()) } answers {
            "test-${args[0]}-${args[1]}"
        }
    }

    fun given(id: Int, result: String) {
        every { instance.getString(id) } returns result
    }
}

fun Int.toTestString() = "test-$this"
