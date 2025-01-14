/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.redaction

import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

class CheckIfEventIsRedactedUseCase @Inject constructor(
        private val session: Session,
) {

    suspend fun execute(roomId: String, eventId: String): Boolean {
        Timber.d("checking if event is redacted for roomId=$roomId and eventId=$eventId")
        return try {
            session.eventService()
                    .getEvent(roomId, eventId)
                    .isRedacted()
                    .also { Timber.d("event isRedacted=$it") }
        } catch (error: Exception) {
            Timber.e(error, "error when getting event, it may not exist yet")
            false
        }
    }
}
