/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import javax.inject.Inject

internal class AddPusherWorker(context: Context, params: WorkerParameters, sessionManager: SessionManager) :
        SessionSafeCoroutineWorker<AddPusherWorker.Params>(context, params, sessionManager, Params::class.java) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val pusher: JsonPusher,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var addPusherTask: AddPusherTask

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        val pusher = params.pusher

        if (pusher.pushKey.isBlank()) {
            return Result.failure()
        }
        return try {
            addPusherTask.execute(AddPusherTask.Params(pusher))
            Result.success()
        } catch (exception: Throwable) {
            when (exception) {
                is Failure.NetworkConnection -> Result.retry()
                else -> Result.failure()
            }
        }
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }
}
