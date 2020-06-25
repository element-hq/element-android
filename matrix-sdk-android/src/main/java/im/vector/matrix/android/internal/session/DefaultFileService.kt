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
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.matrix.android.internal.di.CacheDirectory
import im.vector.matrix.android.internal.di.ExternalFilesDirectory
import im.vector.matrix.android.internal.di.SessionDownloadsDirectory
import im.vector.matrix.android.internal.di.WithProgress
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.toCancelable
import im.vector.matrix.android.internal.util.writeToFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject

internal class DefaultFileService @Inject constructor(
        private val context: Context,
        @CacheDirectory
        private val cacheDirectory: File,
        @ExternalFilesDirectory
        private val externalFilesDirectory: File?,
        @SessionDownloadsDirectory
        private val sessionCacheDirectory: File,
        private val contentUrlResolver: ContentUrlResolver,
        @WithProgress
        private val okHttpClient: OkHttpClient,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor
) : FileService {

    private fun String.safeFileName() = URLEncoder.encode(this, Charsets.US_ASCII.displayName())

    private val downloadFolder = File(sessionCacheDirectory, "MF")

    /**
     * Download file in the cache folder, and eventually decrypt it
     * TODO implement clear file, to delete "MF"
     * TODO looks like files are copied 3 times
     */
    override fun downloadFile(downloadMode: FileService.DownloadMode,
                              id: String,
                              fileName: String,
                              mimeType: String?,
                              url: String?,
                              elementToDecrypt: ElementToDecrypt?,
                              callback: MatrixCallback<File>): Cancelable {
        return taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.io) {
                Try {
                    val unwrappedUrl = url ?: throw IllegalArgumentException("url is null")
                    if (!downloadFolder.exists()) {
                        downloadFolder.mkdirs()
                    }
                    // ensure we use unique file name by using URL (mapped to suitable file name)
                    // Also we need to add extension for the FileProvider, if not it lot's of app that it's
                    // shared with will not function well (even if mime type is passed in the intent)
                    File(downloadFolder, fileForUrl(unwrappedUrl, mimeType))
                }.flatMap { destFile ->
                    if (!destFile.exists()) {
                        val resolvedUrl = contentUrlResolver.resolveFullSize(url) ?: return@flatMap Try.Failure(IllegalArgumentException("url is null"))

                        val request = Request.Builder()
                                .url(resolvedUrl)
                                .header("matrix-sdk:mxc_URL", url ?: "")
                                .build()

                        val response = try {
                            okHttpClient.newCall(request).execute()
                        } catch (e: Throwable) {
                            return@flatMap Try.Failure(e)
                        }

                        if (!response.isSuccessful) {
                            return@flatMap Try.Failure(IOException())
                        }

                        val source = response.body?.source()
                                ?: return@flatMap Try.Failure(IOException())

                        Timber.v("Response size ${response.body?.contentLength()} - Stream available: ${!source.exhausted()}")

                        if (elementToDecrypt != null) {
                            Timber.v("## decrypt file")
                            val decryptedStream = MXEncryptedAttachments.decryptAttachment(source.inputStream(), elementToDecrypt)
                            response.close()
                            if (decryptedStream == null) {
                                return@flatMap Try.Failure(IllegalStateException("Decryption error"))
                            } else {
                                decryptedStream.use {
                                    writeToFile(decryptedStream, destFile)
                                }
                            }
                        } else {
                            writeToFile(source.inputStream(), destFile)
                            response.close()
                        }
                    }

                    Try.just(copyFile(destFile, downloadMode))
                }
            }
                    .foldToCallback(callback)
        }.toCancelable()
    }

    private fun fileForUrl(url: String, mimeType: String?): String {
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) }
        return if (extension != null) "${url.safeFileName()}.$extension" else url.safeFileName()
    }

    override fun isFileInCache(mxcUrl: String, mimeType: String?): Boolean {
        return File(downloadFolder,  fileForUrl(mxcUrl, mimeType)).exists()
    }

    /**
     * Use this URI and pass it to intent using flag Intent.FLAG_GRANT_READ_URI_PERMISSION
     * (if not other app won't be able to access it)
     */
    override fun getTemporarySharableURI(mxcUrl: String, mimeType: String?): Uri? {
        // this string could be extracted no?
        val authority = "${context.packageName}.mx-sdk.fileprovider"
        val targetFile = File(downloadFolder, fileForUrl(mxcUrl, mimeType))
        if (!targetFile.exists()) return null
        return FileProvider.getUriForFile(context, authority, targetFile)
    }

    private fun copyFile(file: File, downloadMode: FileService.DownloadMode): File {
        return when (downloadMode) {
            FileService.DownloadMode.TO_EXPORT          ->
                file.copyTo(File(externalFilesDirectory, file.name), true)
            FileService.DownloadMode.FOR_EXTERNAL_SHARE ->
                file.copyTo(File(File(cacheDirectory, "ext_share"), file.name), true)
            FileService.DownloadMode.FOR_INTERNAL_USE   ->
                file
        }
    }
}
