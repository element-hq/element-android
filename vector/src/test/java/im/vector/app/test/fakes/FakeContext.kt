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

import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.io.OutputStream

class FakeContext(
        private val contentResolver: ContentResolver = mockk()
) {

    val instance = mockk<Context>()

    init {
        every { instance.contentResolver } returns contentResolver
        every { instance.applicationContext } returns instance
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

    fun givenNoConnection() {
        val connectivityManager = FakeConnectivityManager()
        connectivityManager.givenNoActiveConnection()
        givenService(Context.CONNECTIVITY_SERVICE, ConnectivityManager::class.java, connectivityManager.instance)
    }

    fun <T> givenService(name: String, klass: Class<T>, service: T) {
        every { instance.getSystemService(name) } returns service
        every { instance.getSystemService(klass) } returns service
    }

    fun givenHasConnection() {
        val connectivityManager = FakeConnectivityManager()
        connectivityManager.givenHasActiveConnection()
        givenService(Context.CONNECTIVITY_SERVICE, ConnectivityManager::class.java, connectivityManager.instance)
    }

    fun givenStartActivity(intent: Intent) {
        justRun { instance.startActivity(intent) }
    }

    fun verifyStartActivity(intent: Intent) {
        verify { instance.startActivity(intent) }
    }

    fun givenClipboardManager(): FakeClipboardManager {
        val fakeClipboardManager = FakeClipboardManager()
        givenService(Context.CLIPBOARD_SERVICE, ClipboardManager::class.java, fakeClipboardManager.instance)
        return fakeClipboardManager
    }

    fun givenPackageName(name: String) {
        every { instance.packageName } returns name
    }
}
