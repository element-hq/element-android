/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.location

import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SendStaticLocationTask : Task<SendStaticLocationTask.Params, Cancelable> {
    data class Params(
            val roomId: String,
            val latitude: Double,
            val longitude: Double,
            val uncertainty: Double?,
            val isUserLocation: Boolean
    )
}

internal class DefaultSendStaticLocationTask @Inject constructor(
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor,
) : SendStaticLocationTask {

    override suspend fun execute(params: SendStaticLocationTask.Params): Cancelable {
        val event = localEchoEventFactory.createStaticLocationEvent(
                roomId = params.roomId,
                latitude = params.latitude,
                longitude = params.longitude,
                uncertainty = params.uncertainty,
                isUserLocation = params.isUserLocation
        )
        localEchoEventFactory.createLocalEcho(event)
        return eventSenderProcessor.postEvent(event)
    }
}
