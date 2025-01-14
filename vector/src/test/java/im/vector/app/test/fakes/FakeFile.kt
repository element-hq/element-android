/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.net.Uri
import androidx.core.net.toUri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File

class FakeFile {

    val instance = mockk<File>()

    init {
        mockkStatic(Uri::class)
    }

    /**
     * To be called after tests.
     */
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    fun givenName(name: String) {
        every { instance.name } returns name
    }

    fun givenUri(uri: Uri) {
        every { instance.toUri() } returns uri
    }
}
