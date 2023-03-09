/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.detail.domain

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.isPollEnd
import timber.log.Timber
import javax.inject.Inject

class GetEndedPollEventIdUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(roomId: String, startPollEventId: String): String? {
        val result = runCatching {
            activeSessionHolder.getActiveSession().roomService().getRoom(roomId)
                    ?.timelineService()
                    ?.getTimelineEventsRelatedTo(RelationType.REFERENCE, startPollEventId)
                    ?.find { it.root.isPollEnd() }
                    ?.eventId
        }.onFailure { Timber.w("failed to retrieve the ended poll event id for eventId:$startPollEventId") }
        return result.getOrNull()
    }
}
