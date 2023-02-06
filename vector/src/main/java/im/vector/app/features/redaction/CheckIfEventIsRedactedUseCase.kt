/*
 * Copyright (c) 2022 New Vector Ltd
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
