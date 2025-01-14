/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.net.Uri
import io.mockk.every
import io.mockk.mockk

class FakeUri(contentEquals: String? = null) {

    val instance = mockk<Uri>()

    init {
        contentEquals?.let {
            givenEquals(it)
            every { instance.toString() } returns it
            every { instance.scheme } returns contentEquals.substring(0, contentEquals.indexOf(':'))
        }
    }

    fun givenNonHierarchical() {
        givenContent(schema = "mail", path = null)
    }

    fun givenContent(schema: String, path: String?) {
        every { instance.scheme } returns schema
        every { instance.path } returns path
    }

    @Suppress("ReplaceCallWithBinaryOperator")
    fun givenEquals(content: String) {
        every { instance.equals(any()) } answers {
            it.invocation.args.first() == content
        }
        every { instance.hashCode() } answers { content.hashCode() }
    }
}
