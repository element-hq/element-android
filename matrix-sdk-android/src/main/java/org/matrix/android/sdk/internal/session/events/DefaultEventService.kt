/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.events

import org.matrix.android.sdk.api.session.events.EventService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.network.WifiDetector
import org.matrix.android.sdk.internal.session.call.CallEventProcessor
import org.matrix.android.sdk.internal.session.room.timeline.GetEventTask
import timber.log.Timber
import javax.inject.Inject

internal class DefaultEventService @Inject constructor(
        private val getEventTask: GetEventTask,
        private val callEventProcessor: CallEventProcessor,
        private val wifiDetector: WifiDetector
) : EventService {

    override suspend fun getEvent(roomId: String, eventId: String, onlyOnWifi: Boolean): Event? {
        if (onlyOnWifi && !wifiDetector.isConnectedToWifi()) {
            Timber.d("No WiFi network, do not get Event")
            return null
        }

        val event = getEventTask.execute(GetEventTask.Params(roomId, eventId))

        // Fast lane to the call event processors: try to make the incoming call ring faster
        if (callEventProcessor.shouldProcessFastLane(event.getClearType())) {
            callEventProcessor.processFastLane(event)
        }

        return event
    }
}
