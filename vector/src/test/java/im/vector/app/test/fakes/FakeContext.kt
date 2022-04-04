/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.test.fakes

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.mockk.every
import io.mockk.mockk
import java.io.OutputStream

class FakeContext(
        private val contentResolver: ContentResolver = mockk()
) {

    val instance = mockk<Context>()

    init {
        every { instance.contentResolver } returns contentResolver
    }

    fun givenFileDescriptor(uri: Uri, mode: String, factory: () -> ParcelFileDescriptor?) {
        val fileDescriptor = factory()
        every { contentResolver.openFileDescriptor(uri, mode, null) } returns fileDescriptor
    }

    fun givenSafeOutputStreamFor(uri: Uri): OutputStream {
        val outputStream = mockk<OutputStream>(relaxed = true)
        every { contentResolver.openOutputStream(uri, "wt") } returns outputStream
        return outputStream
    }

    fun givenMissingSafeOutputStreamFor(uri: Uri) {
        every { contentResolver.openOutputStream(uri, "wt") } returns null
    }
}
