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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class KeysExporter @Inject constructor(
        private val session: Session,
        private val context: Context
) {
    /**
     * Export keys and write them to the provided uri
     */
    suspend fun export(password: String, uri: Uri) {
        return withContext(Dispatchers.IO) {
            val data = session.cryptoService().exportRoomKeys(password)
            context.contentResolver.openOutputStream(uri)
                    ?.use { it.write(data) }
                    ?: throw IllegalStateException("Unable to open file for writting")
        }
    }
}
