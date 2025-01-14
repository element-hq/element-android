/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.ClipData
import im.vector.app.test.fakes.FakeContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val A_TEXT = "text"

class CopyToClipboardUseCaseTest {

    private val fakeContext = FakeContext()

    private val copyToClipboardUseCase = CopyToClipboardUseCase(
            context = fakeContext.instance
    )

    @Before
    fun setup() {
        mockkStatic(ClipData::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a text when executing the use case then the text is copied into the clipboard`() {
        // Given
        val clipboardManager = fakeContext.givenClipboardManager()
        clipboardManager.givenSetPrimaryClip()
        val clipData = mockk<ClipData>()
        every { ClipData.newPlainText(any(), any()) } returns clipData

        // When
        copyToClipboardUseCase.execute(A_TEXT)

        // Then
        clipboardManager.verifySetPrimaryClip(clipData)
        verify { ClipData.newPlainText("", A_TEXT) }
    }
}
