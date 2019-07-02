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
import android.net.Uri
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.riotx.core.intent.getMimeTypeFromUri
import im.vector.riotx.core.resources.openResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class KeysImporter(private val session: Session) {

    /**
     * Import keys from provided Uri
     */
    fun import(context: Context,
               uri: Uri,
               mimetype: String?,
               password: String,
               callback: MatrixCallback<ImportRoomKeysResult>) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                Try {
                    val resource = openResource(context, uri, mimetype ?: getMimeTypeFromUri(context, uri))

                    if (resource?.mContentStream == null) {
                        throw Exception("Error")
                    }

                    val data: ByteArray
                    try {
                        data = ByteArray(resource.mContentStream!!.available())
                        resource.mContentStream!!.read(data)
                        resource.mContentStream!!.close()

                        data
                    } catch (e: Exception) {
                        try {
                            resource.mContentStream!!.close()
                        } catch (e2: Exception) {
                            Timber.e(e2, "## importKeys()")
                        }

                        throw e
                    }
                }
            }
                    .fold(
                            {
                                callback.onFailure(it)
                            },
                            { byteArray ->
                                session.importRoomKeys(byteArray,
                                        password,
                                        null,
                                        callback)
                            }
                    )
        }
    }
}