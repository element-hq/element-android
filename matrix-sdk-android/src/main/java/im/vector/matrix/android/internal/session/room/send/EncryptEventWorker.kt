/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.crypto.model.MXEncryptEventContentResult
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

internal class EncryptEventWorker(context: Context, params: WorkerParameters)
    : Worker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val userId: String,
            val roomId: String,
            val event: Event
    ) : SessionWorkerParams

    @Inject lateinit var crypto: CryptoService
    @Inject lateinit var localEchoUpdater: LocalEchoUpdater

    override fun doWork(): Result {

        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()

        val sessionComponent = getSessionComponent(params.userId) ?: return Result.success()
        sessionComponent.inject(this)

        val localEvent = params.event
        if (localEvent.eventId == null) {
            return Result.success()
        }
        localEchoUpdater.updateSendState(localEvent.eventId, SendState.ENCRYPTING)

        // TODO Better async handling
        val latch = CountDownLatch(1)

        var result: MXEncryptEventContentResult? = null
        var error: Throwable? = null

        try {
            crypto.encryptEventContent(localEvent.content!!, localEvent.type, params.roomId, object : MatrixCallback<MXEncryptEventContentResult> {
                override fun onSuccess(data: MXEncryptEventContentResult) {
                    result = data
                    latch.countDown()
                }

                override fun onFailure(failure: Throwable) {
                    error = failure
                    latch.countDown()
                }
            })
        } catch (e: Throwable) {
            error = e
            latch.countDown()
        }
        latch.await()

        val safeResult = result
        if (safeResult != null) {
            val encryptedEvent = localEvent.copy(
                    type = safeResult.eventType,
                    content = safeResult.eventContent
            )
            val nextWorkerParams = SendEventWorker.Params(params.userId, params.roomId, encryptedEvent)
            return Result.success(WorkerParamsFactory.toData(nextWorkerParams))
        }
        val safeError = error
        val sendState = when (safeError) {
            is Failure.CryptoError -> SendState.FAILED_UNKNOWN_DEVICES
            else                   -> SendState.UNDELIVERED
        }
        localEchoUpdater.updateSendState(localEvent.eventId, sendState)
        //always return success, or the chain will be stuck for ever!
        return Result.success()
    }
}
