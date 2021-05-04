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

import android.content.Context
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.listeners.ProgressListener
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

internal class VideoCompressor @Inject constructor(private val context: Context) {

    suspend fun compress(videoFile: File,
                         progressListener: ProgressListener?): VideoCompressionResult {
        val destinationFile = withContext(Dispatchers.IO) {
            createDestinationFile()
        }

        val job = Job()

        Timber.d("Compressing: start")
        progressListener?.onProgress(0, 100)

        var result: Int = -1
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
                        job.completeExceptionally(exception)
                    }
                })
                .transcode()

        job.join()

        progressListener?.onProgress(100, 100)

        return when (result) {
            Transcoder.SUCCESS_TRANSCODED -> {
                VideoCompressionResult.Success(destinationFile)
            }
            Transcoder.SUCCESS_NOT_NEEDED -> {
                // Delete now the temporary file
                withContext(Dispatchers.IO) {
                    destinationFile.delete()
                }
                VideoCompressionResult.CompressionNotNeeded
            }
            else                          ->
                throw IllegalStateException("Unknown result: $result")
        }
    }

    private fun createDestinationFile(): File {
        return File.createTempFile(UUID.randomUUID().toString(), null, context.cacheDir)
    }
}
