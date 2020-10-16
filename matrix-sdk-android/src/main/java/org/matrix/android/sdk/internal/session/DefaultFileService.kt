/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import arrow.core.Try
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.crypto.attachments.MXEncryptedAttachments
import org.matrix.android.sdk.internal.di.CacheDirectory
import org.matrix.android.sdk.internal.di.ExternalFilesDirectory
import org.matrix.android.sdk.internal.di.SessionDownloadsDirectory
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificateWithProgress
import org.matrix.android.sdk.internal.session.download.DownloadProgressInterceptor.Companion.DOWNLOAD_PROGRESS_INTERCEPTOR_HEADER
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.toCancelable
import org.matrix.android.sdk.internal.util.writeToFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
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
        @UnauthenticatedWithCertificateWithProgress
        private val okHttpClient: OkHttpClient,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor
) : FileService {

    private fun String.safeFileName() = URLEncoder.encode(this, Charsets.US_ASCII.displayName())

    private val downloadFolder = File(sessionCacheDirectory, "MF")

    /**
     * Retain ongoing downloads to avoid re-downloading and already downloading file
     * map of mxCurl to callbacks
     */
    private val ongoing = mutableMapOf<String, ArrayList<MatrixCallback<File>>>()

    /**
     * Download file in the cache folder, and eventually decrypt it
     * TODO looks like files are copied 3 times
     */
    override fun downloadFile(downloadMode: FileService.DownloadMode,
                              id: String,
                              fileName: String,
                              mimeType: String?,
                              url: String?,
                              elementToDecrypt: ElementToDecrypt?,
                              callback: MatrixCallback<File>): Cancelable {
        val unwrappedUrl = url ?: return NoOpCancellable.also {
            callback.onFailure(IllegalArgumentException("url is null"))
        }

        Timber.v("## FileService downloadFile $unwrappedUrl")

        synchronized(ongoing) {
            val existing = ongoing[unwrappedUrl]
            if (existing != null) {
                Timber.v("## FileService downloadFile is already downloading.. ")
                existing.add(callback)
                return NoOpCancellable
            } else {
                // mark as tracked
                ongoing[unwrappedUrl] = ArrayList()
                // and proceed to download
            }
        }

        return taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.io) {
                Try {
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
                                .header(DOWNLOAD_PROGRESS_INTERCEPTOR_HEADER, url)
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
                            Timber.v("## FileService: decrypt file")
                            val decryptSuccess = destFile.outputStream().buffered().use {
                                MXEncryptedAttachments.decryptAttachment(
                                        source.inputStream(),
                                        elementToDecrypt,
                                        it
                                )
                            }
                            response.close()
                            if (!decryptSuccess) {
                                return@flatMap Try.Failure(IllegalStateException("Decryption error"))
                            }
                        } else {
                            writeToFile(source.inputStream(), destFile)
                            response.close()
                        }
                    } else {
                        Timber.v("## FileService: cache hit for $url")
                    }

                    Try.just(copyFile(destFile, downloadMode))
                }
            }.fold({
                callback.onFailure(it)
                // notify concurrent requests
                val toNotify = synchronized(ongoing) {
                    ongoing[unwrappedUrl]?.also {
                        ongoing.remove(unwrappedUrl)
                    }
                }
                toNotify?.forEach { otherCallbacks ->
                    tryOrNull { otherCallbacks.onFailure(it) }
                }
            }, { file ->
                callback.onSuccess(file)
                // notify concurrent requests
                val toNotify = synchronized(ongoing) {
                    ongoing[unwrappedUrl]?.also {
                        ongoing.remove(unwrappedUrl)
                    }
                }
                Timber.v("## FileService additional to notify ${toNotify?.size ?: 0} ")
                toNotify?.forEach { otherCallbacks ->
                    tryOrNull { otherCallbacks.onSuccess(file) }
                }
            })
        }.toCancelable()
    }

    fun storeDataFor(url: String, mimeType: String?, inputStream: InputStream) {
        val file = File(downloadFolder, fileForUrl(url, mimeType))
        val source = inputStream.source().buffer()
        file.sink().buffer().let { sink ->
            source.use { input ->
                sink.use { output ->
                    output.writeAll(input)
                }
            }
        }
    }

    private fun fileForUrl(url: String, mimeType: String?): String {
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) }
        return if (extension != null) "${url.safeFileName()}.$extension" else url.safeFileName()
    }

    override fun isFileInCache(mxcUrl: String, mimeType: String?): Boolean {
        return File(downloadFolder, fileForUrl(mxcUrl, mimeType)).exists()
    }

    override fun fileState(mxcUrl: String, mimeType: String?): FileService.FileState {
        if (isFileInCache(mxcUrl, mimeType)) return FileService.FileState.IN_CACHE
        val isDownloading = synchronized(ongoing) {
            ongoing[mxcUrl] != null
        }
        return if (isDownloading) FileService.FileState.DOWNLOADING else FileService.FileState.UNKNOWN
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
        // TODO some of this seems outdated, will need to be re-worked
        return when (downloadMode) {
            FileService.DownloadMode.TO_EXPORT          ->
                file.copyTo(File(externalFilesDirectory, file.name), true)
            FileService.DownloadMode.FOR_EXTERNAL_SHARE ->
                file.copyTo(File(File(cacheDirectory, "ext_share"), file.name), true)
            FileService.DownloadMode.FOR_INTERNAL_USE   ->
                file
        }
    }

    override fun getCacheSize(): Int {
        return downloadFolder.walkTopDown()
                .onEnter {
                    Timber.v("Get size of ${it.absolutePath}")
                    true
                }
                .sumBy { it.length().toInt() }
    }

    override fun clearCache() {
        downloadFolder.deleteRecursively()
    }
}
