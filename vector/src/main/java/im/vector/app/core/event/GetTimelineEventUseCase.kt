/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.event

import androidx.lifecycle.asFlow
import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.flow.unwrap
import javax.inject.Inject

class GetTimelineEventUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(roomId: String, eventId: String): Flow<TimelineEvent> {
        return activeSessionHolder.getActiveSession().getRoom(roomId)
                ?.timelineService()
                ?.getTimelineEventLive(eventId)
                ?.asFlow()
                ?.unwrap()
                ?: emptyFlow()
    }
}
