/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.call.vectorCallService
import im.vector.app.features.home.room.detail.timeline.helper.TimelineSettingsFactory
import im.vector.app.features.home.room.detail.timeline.merged.MergedTimelines
import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import javax.inject.Inject

private val secondaryTimelineAllowedTypes = listOf(
        EventType.CALL_HANGUP,
        EventType.CALL_INVITE,
        EventType.CALL_REJECT,
        EventType.CALL_ANSWER
)

class TimelineFactory @Inject constructor(private val session: Session, private val timelineSettingsFactory: TimelineSettingsFactory) {

    fun createTimeline(
            coroutineScope: CoroutineScope,
            mainRoom: Room,
            eventId: String?,
            rootThreadEventId: String?
    ): Timeline {
        val settings = timelineSettingsFactory.create(rootThreadEventId)

        if (!session.vectorCallService.protocolChecker.supportVirtualRooms) {
            return mainRoom.timelineService().createTimeline(eventId, settings)
        }
        val virtualRoomId = session.vectorCallService.userMapper.virtualRoomForNativeRoom(mainRoom.roomId)
        return if (virtualRoomId == null) {
            mainRoom.timelineService().createTimeline(eventId, settings)
        } else {
            val virtualRoom = session.getRoom(virtualRoomId)!!
            MergedTimelines(
                    coroutineScope = coroutineScope,
                    mainTimeline = mainRoom.timelineService().createTimeline(eventId, settings),
                    secondaryTimelineParams = MergedTimelines.SecondaryTimelineParams(
                            timeline = virtualRoom.timelineService().createTimeline(null, settings),
                            shouldFilterTypes = true,
                            allowedTypes = secondaryTimelineAllowedTypes
                    )
            )
        }
    }
}
