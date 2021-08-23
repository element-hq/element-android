/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.content

import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.internal.util.TemporaryFileCreator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class VideoCompressor @Inject constructor(
        private val temporaryFileCreator: TemporaryFileCreator
) {

    suspend fun compress(videoFile: File,
                         progressListener: ProgressListener?): VideoCompressionResult {
        val destinationFile = temporaryFileCreator.create()

        val job = Job()

        Timber.d("Compressing: start")
        progressListener?.onProgress(0, 100)

        var result: Int = -1
        var failure: Throwable? = null
        Transcoder.into(destinationFile.path)
                .addDataSource(videoFile.path)
                .setListener(object : TranscoderListener {
                    override fun onTranscodeProgress(progress: Double) {
                        Timber.d("Compressing: $progress%")
                        progressListener?.onProgress((progress * 100).toInt(), 100)
                    }

                    override fun onTranscodeCompleted(successCode: Int) {
                        Timber.d("Compressing: success: $successCode")
                        result = successCode
                        job.complete()
                    }

                    override fun onTranscodeCanceled() {
                        Timber.d("Compressing: cancel")
                        job.cancel()
                    }

                    override fun onTranscodeFailed(exception: Throwable) {
                        Timber.w(exception, "Compressing: failure")
                        failure = exception
                        job.completeExceptionally(exception)
                    }
                })
                .transcode()

        job.join()

        // Note: job is also cancelled if completeExceptionally() was called
        if (job.isCancelled) {
            // Delete now the temporary file
            deleteFile(destinationFile)
            return when (val finalFailure = failure) {
                null -> {
                    // We do not throw a CancellationException, because it's not critical, we will try to send the original file
                    // Anyway this should never occurs, since we never cancel the return value of transcode()
                    Timber.w("Compressing: A failure occurred")
                    VideoCompressionResult.CompressionCancelled
                }
                else -> {
                    // Compression failure can also be considered as not critical, but let the caller decide
                    Timber.w("Compressing: Job cancelled")
                    VideoCompressionResult.CompressionFailed(finalFailure)
                }
            }
        }

        progressListener?.onProgress(100, 100)

        return when (result) {
            Transcoder.SUCCESS_TRANSCODED -> {
                VideoCompressionResult.Success(destinationFile)
            }
            Transcoder.SUCCESS_NOT_NEEDED -> {
                // Delete now the temporary file
                deleteFile(destinationFile)
                VideoCompressionResult.CompressionNotNeeded
            }
            else                          -> {
                // Should not happen...
                // Delete now the temporary file
                deleteFile(destinationFile)
                Timber.w("Unknown result: $result")
                VideoCompressionResult.CompressionFailed(IllegalStateException("Unknown result: $result"))
            }
        }
    }

    private suspend fun deleteFile(file: File) {
        withContext(Dispatchers.IO) {
            file.delete()
        }
    }
}
