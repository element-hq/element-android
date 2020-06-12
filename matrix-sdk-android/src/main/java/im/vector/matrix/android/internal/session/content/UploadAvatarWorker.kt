/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.content

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

/**
 * Possible previous worker: None
 * Possible next worker    : None
 */
internal class UploadAvatarWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val queryUri: Uri,
            val fileName: String,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @JsonClass(generateAdapter = true)
    internal data class OutputParams(
            override val sessionId: String,
            val imageUrl: String? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var fileUploader: FileUploader

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
                        .also { Timber.e("Unable to parse work parameters") }

        Timber.v("Starting upload media work with params $params")

        if (params.lastFailureMessage != null) {
            // Transmit the error
            return Result.success(inputData)
                    .also { Timber.e("Work cancelled due to input error from parent") }
        }

        // Just defensive code to ensure that we never have an uncaught exception that could break the queue
        return try {
            internalDoWork(params)
        } catch (failure: Throwable) {
            Timber.e(failure)
            handleFailure(params, failure)
        }
    }

    private suspend fun internalDoWork(params: Params): Result {
        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        try {
            val inputStream = context.contentResolver.openInputStream(params.queryUri)
                    ?: return Result.success(
                            WorkerParamsFactory.toData(
                                    params.copy(
                                            lastFailureMessage = "Cannot openInputStream for file: " + params.queryUri.toString()
                                    )
                            )
                    )

            inputStream.use {
                return try {
                    Timber.v("## UploadAvatarWorker - Uploading avatar...")
                    val response = fileUploader.uploadByteArray(inputStream.readBytes(), params.fileName, "image/jpeg")
                    Timber.v("## UploadAvatarWorker - Uploadeded avatar: ${response.contentUri}")
                    handleSuccess(params, response.contentUri)
                } catch (t: Throwable) {
                    Timber.e(t, "## UploadAvatarWorker - Uploading avatar failed...")
                    handleFailure(params, t)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            return Result.success(
                    WorkerParamsFactory.toData(
                            params.copy(
                                    lastFailureMessage = e.localizedMessage
                            )
                    )
            )
        }
    }

    private fun handleFailure(params: Params, failure: Throwable): Result {
        return Result.success(
                WorkerParamsFactory.toData(
                        params.copy(
                                lastFailureMessage = failure.localizedMessage
                        )
                )
        )
    }

    private fun handleSuccess(params: Params, imageUrl: String): Result {
        Timber.v("handleSuccess $imageUrl, work is stopped $isStopped")

        val sendParams = OutputParams(params.sessionId, imageUrl, params.lastFailureMessage)
        return Result.success(WorkerParamsFactory.toData(sendParams))
    }
}
