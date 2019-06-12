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

package im.vector.riotredesign.core.files

import android.app.DownloadManager
import android.content.Context
import okio.Okio
import timber.log.Timber
import java.io.File

/**
 * Save a string to a file with Okio
 * @return true in case of success
 */
fun saveStringToFile(str: String, file: File): Boolean {
    return try {
        val sink = Okio.sink(file)

        val bufferedSink = Okio.buffer(sink)

        bufferedSink.writeString(str, Charsets.UTF_8)

        bufferedSink.close()
        sink.close()

        true
    } catch (e: Exception) {
        Timber.e(e, "Error saving file")
        false
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