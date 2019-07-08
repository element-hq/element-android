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

package im.vector.matrix.android.internal.crypto

import android.content.Context
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.md5
import im.vector.matrix.android.internal.util.writeToFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@SessionScope
internal class FileDecryptor @Inject constructor(private val context: Context,
                                                 private val sessionParams: SessionParams,
                                                 private val contentUrlResolver: ContentUrlResolver,
                                                 private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    val okHttpClient = OkHttpClient()

    fun decryptFile(id: String,
                    fileName: String,
                    url: String,
                    elementToDecrypt: ElementToDecrypt,
                    callback: MatrixCallback<File>) {
        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.io) {
                Try {
                    // Create dir tree:
                    // <cache>/DF/<md5(userId)>/<md5(id)>/
                    val tmpFolderRoot = File(context.cacheDir, "DF")
                    val tmpFolderUser = File(tmpFolderRoot, sessionParams.credentials.userId.md5())
                    val tmpFolder = File(tmpFolderUser, id.md5())

                    if (!tmpFolder.exists()) {
                        tmpFolder.mkdirs()
                    }

                    File(tmpFolder, fileName)
                }.map { destFile ->
                    if (!destFile.exists()) {
                        Try {
                            Timber.v("## decrypt file")

                            val resolvedUrl = contentUrlResolver.resolveFullSize(url) ?: throw IllegalArgumentException("url is null")

                            val request = Request.Builder()
                                    .url(resolvedUrl)
                                    .build()

                            val response = okHttpClient.newCall(request).execute()
                            val inputStream = response.body()?.byteStream()
                            Timber.v("Response size ${response.body()?.contentLength()} - Stream available: ${inputStream?.available()}")
                            if (!response.isSuccessful) {
                                throw IOException()
                            }

                            MXEncryptedAttachments.decryptAttachment(inputStream, elementToDecrypt) ?: throw IllegalStateException("Decryption error")
                        }
                                .map { inputStream ->
                                    writeToFile(inputStream, destFile)
                                }
                    }

                    destFile
                }
            }
                    .foldToCallback(callback)
        }
    }
}