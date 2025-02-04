/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import io.mockk.every
import io.mockk.mockk

class FakeContentResolver {

    val instance = mockk<ContentResolver>()

    fun givenUriResult(uri: Uri, cursor: Cursor?) {
        every { instance.query(uri, null, null, null, null) } returns cursor
    }
}
