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

package im.vector.riotredesign.features.crypto.keys

import android.content.Context
import android.os.Environment
import androidx.annotation.WorkerThread
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.riotredesign.core.files.addEntryToDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File

class KeysExporter(private val session: Session) {

    /**
     * Export keys and return the file path with the callback
     */
    fun export(context: Context, password: String, callback: MatrixCallback<String>) {
        session.exportRoomKeys(password, object : MatrixCallback<ByteArray> {

            override fun onSuccess(data: ByteArray) {
                GlobalScope.launch(Dispatchers.Main) {
                    withContext(Dispatchers.IO) {
                        copyToFile(context, data)
                    }
                            .foldToCallback(callback)
                }
            }

            override fun onFailure(failure: Throwable) {
                callback.onFailure(failure)
            }
        })
    }

    @WorkerThread
    private fun copyToFile(context: Context, data: ByteArray): Try<String> {
        return Try {
            val parentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(parentDir, "riotx-keys-" + System.currentTimeMillis() + ".txt")

            val sink = Okio.sink(file)

            val bufferedSink = Okio.buffer(sink)

            bufferedSink.write(data)

            bufferedSink.close()
            sink.close()

            addEntryToDownloadManager(context, file, "text/plain")

            file.absolutePath
        }
    }
}