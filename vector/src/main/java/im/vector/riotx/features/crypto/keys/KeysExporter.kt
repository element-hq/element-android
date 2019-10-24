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

package im.vector.riotx.features.crypto.keys

import android.content.Context
import android.os.Environment
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.util.awaitCallback
import im.vector.riotx.core.files.addEntryToDownloadManager
import im.vector.riotx.core.files.writeToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class KeysExporter(private val session: Session) {

    /**
     * Export keys and return the file path with the callback
     */
    fun export(context: Context, password: String, callback: MatrixCallback<String>) {
        GlobalScope.launch(Dispatchers.Main) {
            runCatching {
                val data = awaitCallback<ByteArray> { session.exportRoomKeys(password, it) }
                withContext(Dispatchers.IO) {
                    val parentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(parentDir, "riotx-keys-" + System.currentTimeMillis() + ".txt")

                    writeToFile(data, file)

                    addEntryToDownloadManager(context, file, "text/plain")

                    file.absolutePath
                }
            }.foldToCallback(callback)
        }
    }
}
