/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.content.ClipData
import android.content.ClipboardManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class FakeClipboardManager {
    val instance = mockk<ClipboardManager>()

    fun givenSetPrimaryClip() {
        every { instance.setPrimaryClip(any()) } just runs
    }

    fun verifySetPrimaryClip(clipData: ClipData) {
        verify { instance.setPrimaryClip(clipData) }
    }
}
