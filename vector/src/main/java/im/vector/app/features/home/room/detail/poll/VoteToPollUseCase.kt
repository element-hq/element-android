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

package im.vector.app.features.home.room.detail.poll

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import timber.log.Timber
import javax.inject.Inject

class VoteToPollUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(roomId: String, pollEventId: String, optionId: String) {
        // Do not allow to vote unsent local echo of the poll event
        if (LocalEcho.isLocalEchoId(pollEventId)) return

        runCatching {
            val room = activeSessionHolder.getActiveSession().getRoom(roomId)
            room?.getTimelineEvent(pollEventId)?.let { pollTimelineEvent ->
                val currentVote = pollTimelineEvent
                        .annotations
                        ?.pollResponseSummary
                        ?.aggregatedContent
                        ?.myVote
                if (currentVote != optionId) {
                    room.sendService().voteToPoll(
                            pollEventId = pollEventId,
                            answerId = optionId
                    )
                }
            }
        }.onFailure { Timber.w("Failed to vote in poll with id $pollEventId in room with id $roomId") }
    }
}
