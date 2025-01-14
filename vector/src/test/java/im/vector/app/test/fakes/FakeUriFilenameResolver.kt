/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.net.Uri
import im.vector.app.features.onboarding.UriFilenameResolver
import io.mockk.every
import io.mockk.mockk

class FakeUriFilenameResolver {

    val instance = mockk<UriFilenameResolver>()

    fun givenFilename(uri: Uri, name: String?) {
        every { instance.getFilenameFromUri(uri) } returns name
    }
}
