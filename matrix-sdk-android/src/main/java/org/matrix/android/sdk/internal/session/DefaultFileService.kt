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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.crypto.attachments.MXEncryptedAttachments
import org.matrix.android.sdk.internal.di.SessionDownloadsDirectory
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificateWithProgress
import org.matrix.android.sdk.internal.session.download.DownloadProgressInterceptor.Companion.DOWNLOAD_PROGRESS_INTERCEPTOR_HEADER
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.md5
import org.matrix.android.sdk.internal.util.toCancelable
import org.matrix.android.sdk.internal.util.writeToFile
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal class DefaultFileService @Inject constructor(
        private val context: Context,
        @SessionDownloadsDirectory
        private val sessionCacheDirectory: File,
        private val contentUrlResolver: ContentUrlResolver,
        @UnauthenticatedWithCertificateWithProgress
        private val okHttpClient: OkHttpClient,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor
) : FileService {

    // Legacy folder, will be deleted
    private val legacyFolder = File(sessionCacheDirectory, "MF")
    // Folder to store downloaded files (not decrypted)
    private val downloadFolder = File(sessionCacheDirectory, "F")
    // Folder to store decrypted files
    private val decryptedFolder = File(downloadFolder, "D")

    init {
        // Clear the legacy downloaded files
        legacyFolder.deleteRecursively()
    }

    /**
     * Retain ongoing downloads to avoid re-downloading and already downloading file
     * map of mxCurl to callbacks
     */
    private val ongoing = mutableMapOf<String, ArrayList<MatrixCallback<File>>>()

    /**
     * Download file in the cache folder, and eventually decrypt it
     * TODO looks like files are copied 3 times
     */
    override fun downloadFile(fileName: String,
                              mimeType: String?,
                              url: String?,
                              elementToDecrypt: ElementToDecrypt?,
                              callback: MatrixCallback<File>): Cancelable {
        url ?: return NoOpCancellable.also {
            callback.onFailure(IllegalArgumentException("url is null"))
        }

        Timber.v("## FileService downloadFile $url")

        synchronized(ongoing) {
            val existing = ongoing[url]
            if (existing != null) {
                Timber.v("## FileService downloadFile is already downloading.. ")
                existing.add(callback)
                return NoOpCancellable
            } else {
                // mark as tracked
                ongoing[url] = ArrayList()
                // and proceed to download
            }
        }

        return taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.io) {
                Try {
                    if (!decryptedFolder.exists()) {
                        decryptedFolder.mkdirs()
                    }
                    // ensure we use unique file name by using URL (mapped to suitable file name)
                    // Also we need to add extension for the FileProvider, if not it lot's of app that it's
                    // shared with will not function well (even if mime type is passed in the intent)
                    getFiles(url, fileName, mimeType, elementToDecrypt != null)
                }.flatMap { cachedFiles ->
                    if (!cachedFiles.file.exists()) {
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

                        // Write the file to cache (encrypted version if the file is encrypted)
                        writeToFile(source.inputStream(), cachedFiles.file)
                        response.close()
                    } else {
                        Timber.v("## FileService: cache hit for $url")
                    }

                    Try.just(cachedFiles)
                }
            }.flatMap { cachedFiles ->
                // Decrypt if necessary
                if (cachedFiles.decryptedFile != null) {
                    if (!cachedFiles.decryptedFile.exists()) {
                        Timber.v("## FileService: decrypt file")
                        // Ensure the parent folder exists
                        cachedFiles.decryptedFile.parentFile?.mkdirs()
                        val decryptSuccess = cachedFiles.file.inputStream().use { inputStream ->
                            cachedFiles.decryptedFile.outputStream().buffered().use { outputStream ->
                                MXEncryptedAttachments.decryptAttachment(
                                        inputStream,
                                        elementToDecrypt,
                                        outputStream
                                )
                            }
                        }
                        if (!decryptSuccess) {
                            return@flatMap Try.Failure(IllegalStateException("Decryption error"))
                        }
                    } else {
                        Timber.v("## FileService: cache hit for decrypted file")
                    }
                    Try.just(cachedFiles.decryptedFile)
                } else {
                    // Clear file
                    Try.just(cachedFiles.file)
                }
            }.fold(
                    { throwable ->
                        callback.onFailure(throwable)
                        // notify concurrent requests
                        val toNotify = synchronized(ongoing) {
                            ongoing[url]?.also {
                                ongoing.remove(url)
                            }
                        }
                        toNotify?.forEach { otherCallbacks ->
                            tryOrNull { otherCallbacks.onFailure(throwable) }
                        }
                    },
                    { file ->
                        callback.onSuccess(file)
                        // notify concurrent requests
                        val toNotify = synchronized(ongoing) {
                            ongoing[url]?.also {
                                ongoing.remove(url)
                            }
                        }
                        Timber.v("## FileService additional to notify ${toNotify?.size ?: 0} ")
                        toNotify?.forEach { otherCallbacks ->
                            tryOrNull { otherCallbacks.onSuccess(file) }
                        }
                    }
            )
        }.toCancelable()
    }

    fun storeDataFor(mxcUrl: String,
                     filename: String?,
                     mimeType: String?,
                     originalFile: File,
                     encryptedFile: File?) {
        val files = getFiles(mxcUrl, filename, mimeType, encryptedFile != null)
        if (encryptedFile != null) {
            // We switch the two files here, original file it the decrypted file
            files.decryptedFile?.let { originalFile.copyTo(it) }
            encryptedFile.copyTo(files.file)
        } else {
            // Just copy the original file
            originalFile.copyTo(files.file)
        }
    }

    private fun safeFileName(fileName: String?, mimeType: String?): String {
        return buildString {
            // filename has to be safe for the Android System
            val result = fileName
                    ?.replace("[^a-z A-Z0-9\\\\.\\-]".toRegex(), "_")
                    ?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_FILENAME
            append(result)
            // Check that the extension is correct regarding the mimeType
            val extensionFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) }
            if (extensionFromMime != null) {
                // Compare
                val fileExtension = result.substringAfterLast(delimiter = ".", missingDelimiterValue = "")
                if (fileExtension.isEmpty() || fileExtension != extensionFromMime) {
                    // Missing extension, or diff in extension, add the one provided by the mimetype
                    append(".")
                    append(extensionFromMime)
                }
            }
        }
    }

    override fun isFileInCache(mxcUrl: String?,
                               fileName: String,
                               mimeType: String?,
                               elementToDecrypt: ElementToDecrypt?): Boolean {
        return fileState(mxcUrl, fileName, mimeType, elementToDecrypt) == FileService.FileState.IN_CACHE
    }

    internal data class CachedFiles(
            // This is the downloaded file. Can be clear or encrypted
            val file: File,
            // This is the decrypted file. Null if the original file is not encrypted
            val decryptedFile: File?
    ) {
        fun getClearFile(): File = decryptedFile ?: file
    }

    private fun getFiles(mxcUrl: String,
                         fileName: String?,
                         mimeType: String?,
                         isEncrypted: Boolean): CachedFiles {
        val hashFolder = mxcUrl.md5()
        val safeFileName = safeFileName(fileName, mimeType)
        return if (isEncrypted) {
            // Encrypted file
            CachedFiles(
                    File(downloadFolder, "$hashFolder/$ENCRYPTED_FILENAME"),
                    File(decryptedFolder, "$hashFolder/$safeFileName")
            )
        } else {
            // Clear file
            CachedFiles(
                    File(downloadFolder, "$hashFolder/$safeFileName"),
                    null
            )
        }
    }

    override fun fileState(mxcUrl: String?,
                           fileName: String,
                           mimeType: String?,
                           elementToDecrypt: ElementToDecrypt?): FileService.FileState {
        mxcUrl ?: return FileService.FileState.UNKNOWN
        if (getFiles(mxcUrl, fileName, mimeType, elementToDecrypt != null).file.exists()) return FileService.FileState.IN_CACHE
        val isDownloading = synchronized(ongoing) {
            ongoing[mxcUrl] != null
        }
        return if (isDownloading) FileService.FileState.DOWNLOADING else FileService.FileState.UNKNOWN
    }

    /**
     * Use this URI and pass it to intent using flag Intent.FLAG_GRANT_READ_URI_PERMISSION
     * (if not other app won't be able to access it)
     */
    override fun getTemporarySharableURI(mxcUrl: String?,
                                         fileName: String,
                                         mimeType: String?,
                                         elementToDecrypt: ElementToDecrypt?): Uri? {
        mxcUrl ?: return null
        // this string could be extracted no?
        val authority = "${context.packageName}.mx-sdk.fileprovider"
        val targetFile = getFiles(mxcUrl, fileName, mimeType, elementToDecrypt != null).getClearFile()
        if (!targetFile.exists()) return null
        return FileProvider.getUriForFile(context, authority, targetFile)
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

    override fun clearDecryptedCache() {
        decryptedFolder.deleteRecursively()
    }

    companion object {
        private const val ENCRYPTED_FILENAME = "encrypted.bin"
        // The extension would be added from the mimetype
        private const val DEFAULT_FILENAME = "file"
    }
}
