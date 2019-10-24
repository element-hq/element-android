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

package im.vector.matrix.android.internal.session

import android.content.Context
import android.os.Environment
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.extensions.foldToCallback
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

internal class DefaultFileService @Inject constructor(private val context: Context,
                                                      @UserMd5 private val userMd5: String,
                                                      private val contentUrlResolver: ContentUrlResolver,
                                                      private val coroutineDispatchers: MatrixCoroutineDispatchers) : FileService {

    val okHttpClient = OkHttpClient()

    /**
     * Download file in the cache folder, and eventually decrypt it
     * TODO implement clear file, to delete "MF"
     */
    override fun downloadFile(downloadMode: FileService.DownloadMode,
                              id: String,
                              fileName: String,
                              url: String?,
                              elementToDecrypt: ElementToDecrypt?,
                              callback: MatrixCallback<File>) {
        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.io) {
                Try {
                    val folder = getFolder(downloadMode, id)

                    File(folder, fileName)
                }.flatMap { destFile ->
                    if (!destFile.exists() || downloadMode == FileService.DownloadMode.TO_EXPORT) {
                        Try {
                            val resolvedUrl = contentUrlResolver.resolveFullSize(url) ?: throw IllegalArgumentException("url is null")

                            val request = Request.Builder()
                                    .url(resolvedUrl)
                                    .build()

                            val response = okHttpClient.newCall(request).execute()
                            val inputStream = response.body?.byteStream()
                            Timber.v("Response size ${response.body?.contentLength()} - Stream available: ${inputStream?.available()}")
                            if (!response.isSuccessful
                                    || inputStream == null) {
                                throw IOException()
                            }

                            if (elementToDecrypt != null) {
                                Timber.v("## decrypt file")
                                MXEncryptedAttachments.decryptAttachment(inputStream, elementToDecrypt) ?: throw IllegalStateException("Decryption error")
                            } else {
                                inputStream
                            }
                        }
                                .map { inputStream ->
                                    writeToFile(inputStream, destFile)
                                    destFile
                                }
                    } else {
                        Try.just(destFile)
                    }
                }
            }
                    .foldToCallback(callback)
        }
    }

    private fun getFolder(downloadMode: FileService.DownloadMode, id: String): File {
        return when (downloadMode) {
            FileService.DownloadMode.FOR_INTERNAL_USE -> {
                // Create dir tree (MF stands for Matrix File):
                // <cache>/MF/<md5(userId)>/<md5(id)>/
                val tmpFolderRoot = File(context.cacheDir, "MF")
                val tmpFolderUser = File(tmpFolderRoot, userMd5)
                File(tmpFolderUser, id.md5())
            }
            FileService.DownloadMode.TO_EXPORT        -> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
        }
                .also { folder ->
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                }
    }
}
