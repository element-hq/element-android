/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.files

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File

/**
 * Save a string to a file with Okio.
 */
@WorkerThread
@Throws
fun writeToFile(str: String, file: File) {
    file.sink().buffer().use {
        it.writeString(str, Charsets.UTF_8)
    }
}

/**
 * Save a byte array to a file with Okio.
 */
@WorkerThread
@Throws
fun writeToFile(data: ByteArray, file: File) {
    file.sink().buffer().use {
        it.write(data)
    }
}

@SuppressLint("Recycle")
fun addEntryToDownloadManager(
        context: Context,
        file: File,
        mimeType: String,
        title: String = file.name,
        description: String = file.name
): Uri? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.TITLE, title)
                put(MediaStore.Downloads.DISPLAY_NAME, description)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.SIZE, file.length())
            }
            return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?.let { uri ->
                        Timber.v("## addEntryToDownloadManager(): $uri")
                        val source = file.inputStream().source().buffer()
                        context.contentResolver.openOutputStream(uri)?.sink()?.buffer()?.let { sink ->
                            source.use { input ->
                                sink.use { output ->
                                    output.writeAll(input)
                                }
                            }
                        }
                        uri
                    } ?: run {
                Timber.v("## addEntryToDownloadManager(): context.contentResolver.insert failed")

                null
            }
        } else {
            val downloadManager = context.getSystemService<DownloadManager>()
            @Suppress("DEPRECATION")
            downloadManager?.addCompletedDownload(title, description, true, mimeType, file.absolutePath, file.length(), true)
            return null
        }
    } catch (e: Exception) {
        Timber.e(e, "## addEntryToDownloadManager(): Exception")
    }
    return null
}
