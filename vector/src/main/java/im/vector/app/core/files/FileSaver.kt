/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.core.files

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import arrow.core.Try
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File

/**
 * Save a string to a file with Okio
 */
@WorkerThread
fun writeToFile(str: String, file: File): Try<Unit> {
    return Try<Unit> {
        file.sink().buffer().use {
            it.writeString(str, Charsets.UTF_8)
        }
    }
}

/**
 * Save a byte array to a file with Okio
 */
@WorkerThread
fun writeToFile(data: ByteArray, file: File): Try<Unit> {
    return Try<Unit> {
        file.sink().buffer().use {
            it.write(data)
        }
    }
}

fun addEntryToDownloadManager(context: Context,
                              file: File,
                              mimeType: String,
                              title: String = file.name,
                              description: String = file.name): Uri? {
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
