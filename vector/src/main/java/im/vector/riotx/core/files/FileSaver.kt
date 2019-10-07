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

package im.vector.riotx.core.files

import android.app.DownloadManager
import android.content.Context
import androidx.annotation.WorkerThread
import arrow.core.Try
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File

/**
 * Save a string to a file with Okio
 */
@WorkerThread
fun writeToFile(str: String, file: File): Try<Unit> {
    return Try {
        val sink = file.sink()

        val bufferedSink = sink.buffer()

        bufferedSink.writeString(str, Charsets.UTF_8)

        bufferedSink.close()
        sink.close()
    }
}

/**
 * Save a byte array to a file with Okio
 */
@WorkerThread
fun writeToFile(data: ByteArray, file: File): Try<Unit> {
    return Try {
        val sink = file.sink()

        val bufferedSink = sink.buffer()

        bufferedSink.write(data)

        bufferedSink.close()
        sink.close()
    }
}

fun addEntryToDownloadManager(context: Context,
                              file: File,
                              mimeType: String,
                              title: String = file.name,
                              description: String = file.name) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?

    try {
        downloadManager?.addCompletedDownload(title, description, true, mimeType, file.absolutePath, file.length(), true)
    } catch (e: Exception) {
        Timber.e(e, "## addEntryToDownloadManager(): Exception")
    }
}