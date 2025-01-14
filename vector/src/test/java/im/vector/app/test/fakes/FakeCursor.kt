/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.database.Cursor
import io.mockk.every
import io.mockk.mockk

class FakeCursor {

    val instance = mockk<Cursor>()

    init {
        every { instance.close() } answers {}
    }

    fun givenEmpty() {
        every { instance.count } returns 0
        every { instance.moveToFirst() } returns false
    }

    fun givenString(columnName: String, content: String?) {
        val columnId = columnName.hashCode()
        every { instance.moveToFirst() } returns true
        every { instance.isNull(columnId) } returns (content == null)
        every { instance.getColumnIndex(columnName) } returns columnId
        every { instance.getString(columnId) } returns content
    }
}
