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
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

internal class VideoCompressor @Inject constructor(private val context: Context) {
    suspend fun compress(videoFile: File,
                         quality: VideoQuality = VideoQuality.MEDIUM,
                         isMinBitRateEnabled: Boolean = false,
                         keepOriginalResolution: Boolean = true): File {
        return withContext(Dispatchers.IO) {
            val job = Job()
            val destinationFile = createDestinationFile()

            // Sadly it does not return the Job, the API is not ideal
            VideoCompressor.start(
                    context = null,
                    srcUri = null,
                    srcPath = videoFile.path,
                    destPath = destinationFile.path,
                    listener = object : CompressionListener {
                        override fun onProgress(percent: Float) {
                            Timber.d("Compressing: $percent%")
                        }

                        override fun onStart() {
                            Timber.d("Compressing: start")
                        }

                        override fun onSuccess() {
                            Timber.d("Compressing: success")
                            job.complete()
                        }

                        override fun onFailure(failureMessage: String) {
                            Timber.d("Compressing: failure: $failureMessage")
                            job.completeExceptionally(Exception(failureMessage))
                        }

                        override fun onCancelled() {
                            Timber.d("Compressing: cancel")
                            job.cancel()
                        }
                    },
                    quality = quality,
                    isMinBitRateEnabled = isMinBitRateEnabled,
                    keepOriginalResolution = keepOriginalResolution
            )

            job.join()
            destinationFile
        }
    }

    private fun createDestinationFile(): File {
        return File.createTempFile(UUID.randomUUID().toString(), null, context.cacheDir)
    }
}
