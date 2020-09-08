/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.room.relation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.greenrobot.eventbus.EventBus
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.relation.ReactionContent
import org.matrix.android.sdk.api.session.room.model.relation.ReactionInfo
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.send.SendResponse
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

// TODO This is not used. Delete?
internal class SendRelationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val roomId: String,
            val event: Event,
            val relationType: String? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var roomAPI: RoomAPI
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()
                        .also { Timber.e("Unable to parse work parameters") }

        if (params.lastFailureMessage != null) {
            // Transmit the error
            return Result.success(inputData)
                    .also { Timber.e("Work cancelled due to input error from parent") }
        }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val localEvent = params.event
        if (localEvent.eventId == null) {
            return Result.failure()
        }
        val relationContent = localEvent.content.toModel<ReactionContent>()
                ?: return Result.failure()
        val relatedEventId = relationContent.relatesTo?.eventId ?: return Result.failure()
        val relationType = (relationContent.relatesTo as? ReactionInfo)?.type ?: params.relationType
        ?: return Result.failure()
        return try {
            sendRelation(params.roomId, relationType, relatedEventId, localEvent)
            Result.success()
        } catch (exception: Throwable) {
            when (exception) {
                is Failure.NetworkConnection -> Result.retry()
                else                         -> {
                    // TODO mark as failed to send?
                    // always return success, or the chain will be stuck for ever!
                    Result.success()
                }
            }
        }
    }

    private suspend fun sendRelation(roomId: String, relationType: String, relatedEventId: String, localEvent: Event) {
        executeRequest<SendResponse>(eventBus) {
            apiCall = roomAPI.sendRelation(
                    roomId = roomId,
                    parentId = relatedEventId,
                    relationType = relationType,
                    eventType = localEvent.type,
                    content = localEvent.content
            )
        }
    }
}
