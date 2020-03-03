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

import android.os.Environment
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.matrix.android.internal.di.SessionCacheDirectory
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.toCancelable
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

internal class DefaultFileService @Inject constructor(
        @SessionCacheDirectory
        private val cacheDirectory: File,
        @SessionFilesDirectory
        private val filesDirectory: File,
        private val contentUrlResolver: ContentUrlResolver,
        @Unauthenticated
        private val okHttpClient: OkHttpClient,
        private val coroutineDispatchers: MatrixCoroutineDispatchers) : FileService {

    /**
     * Download file in the cache folder, and eventually decrypt it
     * TODO implement clear file, to delete "MF"
     */
    override fun downloadFile(downloadMode: FileService.DownloadMode,
                              id: String,
                              fileName: String,
                              url: String?,
                              elementToDecrypt: ElementToDecrypt?,
                              callback: MatrixCallback<File>): Cancelable {
        return GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.io) {
                Try {
                    val folder = File(cacheDirectory, "MF")
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                    File(folder, fileName)
                }.flatMap { destFile ->
                    if (!destFile.exists()) {
                        val resolvedUrl = contentUrlResolver.resolveFullSize(url) ?: throw IllegalArgumentException("url is null")

                        val request = Request.Builder()
                                .url(resolvedUrl)
                                .build()

                        val response = okHttpClient.newCall(request).execute()
                        var inputStream = response.body?.byteStream()
                        Timber.v("Response size ${response.body?.contentLength()} - Stream available: ${inputStream?.available()}")
                        if (!response.isSuccessful || inputStream == null) {
                            return@flatMap Try.Failure(IOException())
                        }

                        if (elementToDecrypt != null) {
                            Timber.v("## decrypt file")
                            inputStream = MXEncryptedAttachments.decryptAttachment(inputStream, elementToDecrypt)
                                    ?: return@flatMap Try.Failure(IllegalStateException("Decryption error"))
                        }

                        writeToFile(inputStream, destFile)
                    }

                    Try.just(copyFile(destFile, downloadMode))
                }
            }
                    .foldToCallback(callback)
        }.toCancelable()
    }

    private fun copyFile(file: File, downloadMode: FileService.DownloadMode): File {
        return when (downloadMode) {
            FileService.DownloadMode.TO_EXPORT          -> file.copyTo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), true)
            FileService.DownloadMode.FOR_INTERNAL_USE   -> file.copyTo(File(filesDirectory, "ext_share"), true)
            FileService.DownloadMode.FOR_EXTERNAL_SHARE -> file
        }
    }
}
