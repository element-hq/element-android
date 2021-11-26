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

package org.matrix.android.sdk.internal.worker

import android.content.Context
import androidx.annotation.CallSuper
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.session.SessionComponent
import timber.log.Timber

/**
 * This worker should only sends Result.Success when added to a unique queue to avoid breaking the unique queue.
 * This abstract class handle the cases of problem when parsing parameter, and forward the error if any to
 * the next workers.
 */
internal abstract class SessionSafeCoroutineWorker<PARAM : SessionWorkerParams>(
        context: Context,
        workerParameters: WorkerParameters,
        private val sessionManager: SessionManager,
        private val paramClass: Class<PARAM>
) : CoroutineWorker(context, workerParameters) {

    @JsonClass(generateAdapter = true)
    internal data class ErrorData(
            override val sessionId: String,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    final override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData(paramClass, inputData)
                ?: return buildErrorResult(null, "Unable to parse work parameters")
                        .also { Timber.e("Unable to parse work parameters") }

        return try {
            val sessionComponent = sessionManager.getSessionComponent(params.sessionId)
                    ?: return buildErrorResult(params, "No session")

            // Make sure to inject before handling error as you may need some dependencies to process them.
            injectWith(sessionComponent)
            if (params.lastFailureMessage != null) {
                // Forward error to the next workers
                doOnError(params)
            } else {
                doSafeWork(params)
            }
        } catch (throwable: Throwable) {
            buildErrorResult(params, throwable.localizedMessage ?: "error")
        }
    }

    abstract fun injectWith(injector: SessionComponent)

    /**
     * Should only return Result.Success for workers added to a unique queue
     */
    abstract suspend fun doSafeWork(params: PARAM): Result

    protected fun buildErrorResult(params: PARAM?, message: String): Result {
        return Result.success(
                if (params != null) {
                    WorkerParamsFactory.toData(paramClass, buildErrorParams(params, message))
                } else {
                    WorkerParamsFactory.toData(ErrorData::class.java, ErrorData(sessionId = "", lastFailureMessage = message))
                }
        )
    }

    abstract fun buildErrorParams(params: PARAM, message: String): PARAM

    /**
     * This is called when the input parameters are correct, but contain an error from the previous worker.
     */
    @CallSuper
    open fun doOnError(params: PARAM): Result {
        // Forward the error
        return Result.success(inputData)
                .also { Timber.e("Work cancelled due to input error from parent") }
    }

    companion object {
        fun hasFailed(outputData: Data): Boolean {
            return WorkerParamsFactory.fromData(ErrorData::class.java, outputData)
                    .let { it?.lastFailureMessage != null }
        }
    }
}
