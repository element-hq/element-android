/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
