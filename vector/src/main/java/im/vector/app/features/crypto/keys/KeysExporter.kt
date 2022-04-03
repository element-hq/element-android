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

package im.vector.app.features.crypto.keys

import android.content.Context
import android.net.Uri
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.extensions.safeOpenOutputStream
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class KeysExporter @Inject constructor(
        private val session: Session,
        private val context: Context,
        private val dispatchers: CoroutineDispatchers
) {
    /**
     * Export keys and write them to the provided uri
     */
    suspend fun export(password: String, uri: Uri) {
        withContext(dispatchers.io) {
            val data = session.cryptoService().exportRoomKeys(password)
            context.safeOpenOutputStream(uri)
                    ?.use { it.write(data) }
                    ?: throw IllegalStateException("Unable to open file for writing")
            verifyExportedKeysOutputFileSize(uri, expectedSize = data.size.toLong())
        }
    }

    private fun verifyExportedKeysOutputFileSize(uri: Uri, expectedSize: Long) {
        val output = context.contentResolver.openFileDescriptor(uri, "r", null)
        when {
            output == null                  -> throw IllegalStateException("Exported file not found")
            output.statSize != expectedSize -> {
                throw UnexpectedExportKeysFileSizeException(
                        expectedFileSize = expectedSize,
                        actualFileSize = output.statSize
                )
            }
        }
    }
}

class UnexpectedExportKeysFileSizeException(expectedFileSize: Long, actualFileSize: Long) : IllegalStateException(
        "Exported Keys file has unexpected file size, got: $actualFileSize but expected: $expectedFileSize"
)
