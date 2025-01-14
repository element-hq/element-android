/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
