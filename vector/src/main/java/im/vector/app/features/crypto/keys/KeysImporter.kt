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
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.resources.openResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import javax.inject.Inject

class KeysImporter @Inject constructor(
        private val context: Context,
        private val session: Session
) {
    /**
     * Import keys from provided Uri
     */
    suspend fun import(uri: Uri,
                       mimetype: String?,
                       password: String): ImportRoomKeysResult {
        return withContext(Dispatchers.IO) {
            val resource = openResource(context, uri, mimetype ?: getMimeTypeFromUri(context, uri))
            val stream = resource?.mContentStream ?: throw Exception("Error")
            val data = stream.use { it.readBytes() }
            session.cryptoService().importRoomKeys(data, password, null)
        }
    }
}
