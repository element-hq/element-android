/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.element.android.opusencoder.OggOpusEncoder
import io.mockk.every
import io.mockk.mockk
import java.io.File

class FakeOggOpusEncoder : OggOpusEncoder by mockk() {

    init {
        every { init(any(), any()) } returns 0
        every { setBitrate(any()) } returns 0
        every { encode(any(), any()) } returns 0
        every { release() } answers {}
    }

    fun createEmptyFileOnInit() {
        every { init(any(), any()) } answers {
            val filePath = arg<String>(0)
            if (File(filePath).createNewFile()) 0 else 1
        }
    }
}
