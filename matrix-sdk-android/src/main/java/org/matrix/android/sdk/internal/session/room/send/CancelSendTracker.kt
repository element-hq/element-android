/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
