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

package im.vector.app.features.crypto.keys

import android.net.Uri
import android.os.ParcelFileDescriptor
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeCryptoService
import im.vector.app.test.fakes.FakeSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertFailsWith
import org.junit.Before
import org.junit.Test

private val A_URI = mockk<Uri>()
private val A_ROOM_KEYS_EXPORT = ByteArray(size = 111)
private const val A_PASSWORD = "a password"

class KeysExporterTest {

    private val cryptoService = FakeCryptoService()
    private val context = FakeContext()
    private val keysExporter = KeysExporter(
            session = FakeSession(fakeCryptoService = cryptoService),
            context = context.instance,
            dispatchers = CoroutineDispatchers(Dispatchers.Unconfined, Dispatchers.Unconfined)
    )

    @Before
    fun setUp() {
        cryptoService.roomKeysExport = A_ROOM_KEYS_EXPORT
    }

    @Test
    fun `when exporting then writes exported keys to context output stream`() {
        givenFileDescriptorWithSize(size = A_ROOM_KEYS_EXPORT.size.toLong())
        val outputStream = context.givenSafeOutputStreamFor(A_URI)

        runTest { keysExporter.export(A_PASSWORD, A_URI) }

        verify { outputStream.write(A_ROOM_KEYS_EXPORT) }
    }

    @Test
    fun `given different file size returned for export when exporting then throws UnexpectedExportKeysFileSizeException`() {
        givenFileDescriptorWithSize(size = 110)
        context.givenSafeOutputStreamFor(A_URI)

        assertFailsWith<UnexpectedExportKeysFileSizeException> {
            runTest { keysExporter.export(A_PASSWORD, A_URI) }
        }
    }

    @Test
    fun `given output stream is unavailable for exporting to when exporting then throws IllegalStateException`() {
        context.givenMissingSafeOutputStreamFor(A_URI)

        assertFailsWith<IllegalStateException>(message = "Unable to open file for writing") {
            runTest { keysExporter.export(A_PASSWORD, A_URI) }
        }
    }

    @Test
    fun `given exported file is missing after export when exporting then throws IllegalStateException`() {
        context.givenFileDescriptor(A_URI, mode = "r") { null }
        context.givenSafeOutputStreamFor(A_URI)

        assertFailsWith<IllegalStateException>(message = "Exported file not found") {
            runTest { keysExporter.export(A_PASSWORD, A_URI) }
        }
    }

    private fun givenFileDescriptorWithSize(size: Long) {
        context.givenFileDescriptor(A_URI, mode = "r") {
            mockk<ParcelFileDescriptor>().also { every { it.statSize } returns size }
        }
    }
}
