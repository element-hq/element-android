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

package org.matrix.android.sdk.internal.session.room.send

import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

/**
 * We cannot use work manager cancellation mechanism because cancelling a work will just ignore
 * any follow up send that was already queued.
 * We use this class to track cancel requests, the workers will look for this to check for cancellation request
 * and just ignore the work request and continue by returning success.
 *
 * Known limitation, for now requests are not persisted
 */
@SessionScope
internal class CancelSendTracker @Inject constructor() {

    data class Request(
            val localId: String,
            val roomId: String
    )

    private val cancellingRequests = ArrayList<Request>()

    fun markLocalEchoForCancel(eventId: String, roomId: String) {
        synchronized(cancellingRequests) {
            cancellingRequests.add(Request(eventId, roomId))
        }
    }

    fun isCancelRequestedFor(eventId: String?, roomId: String?): Boolean {
        val found = synchronized(cancellingRequests) {
            cancellingRequests.any { it.localId == eventId && it.roomId == roomId }
        }
        return found
    }

    fun markCancelled(eventId: String, roomId: String) {
        synchronized(cancellingRequests) {
            val index = cancellingRequests.indexOfFirst { it.localId == eventId && it.roomId == roomId }
            if (index != -1) {
                cancellingRequests.removeAt(index)
            }
        }
    }
}
