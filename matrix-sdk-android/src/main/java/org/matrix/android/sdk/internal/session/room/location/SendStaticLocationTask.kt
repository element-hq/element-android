/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
