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

package org.matrix.android.sdk.internal.crypto

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.toDebugString
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.createUniqueTxnId
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import timber.log.Timber
import javax.inject.Inject

internal class SimpleSendToDeviceWorker(context: Context,
                                        params: WorkerParameters) :
        SessionSafeCoroutineWorker<SimpleSendToDeviceWorker.Params>(context, params, Params::class.java) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val eventType: String,
            val contentMap: Map<String /* userId */, Map<String /* deviceId */, Any>>,
            val txnId: String? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var sendToDeviceTask: SendToDeviceTask

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        // params.txnId should be provided in all cases now. But Params can be deserialized by
        // the WorkManager from data serialized in a previous version of the application, so without the txnId field.
        // So if not present, we create a txnId
        val txnId = params.txnId ?: createUniqueTxnId()
        val eventType: String = params.eventType

        val sendToDeviceMap = MXUsersDevicesMap<Any>().apply { addEntriesFromRawMap(params.contentMap) }

        try {
            Timber.d("## CRYPTO | Worker shareUserDevicesKey() $txnId: Sharing megolm session with ${sendToDeviceMap.toDebugString()} ")
            sendToDeviceTask.execute(
                    SendToDeviceTask.Params(
                            eventType = eventType,
                            contentMap = sendToDeviceMap,
                            transactionId = txnId
                    )
            )
            Timber.d("## CRYPTO | Worker shareUserDevicesKey() $txnId ... success")
            return Result.success()
        } catch (throwable: Throwable) {
            return if (throwable.shouldBeRetried()) {
                Timber.d("## CRYPTO | Worker shareUserDevicesKey() $txnId ... schedule retry")
                Result.retry()
            } else {
                Timber.d("## CRYPTO | Worker shareUserDevicesKey() $txnId ... failure")
                buildErrorResult(params, throwable.localizedMessage ?: "error")
            }
        }
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }
}
