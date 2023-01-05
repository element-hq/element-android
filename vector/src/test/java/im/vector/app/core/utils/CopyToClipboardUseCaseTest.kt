/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
