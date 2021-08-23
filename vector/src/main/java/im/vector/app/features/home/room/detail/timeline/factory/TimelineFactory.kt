/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.call.vectorCallService
import im.vector.app.features.home.room.detail.timeline.helper.TimelineSettingsFactory
import im.vector.app.features.home.room.detail.timeline.merged.MergedTimelines
import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
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

    fun createTimeline(coroutineScope: CoroutineScope, mainRoom: Room, eventId: String?): Timeline {
        val settings = timelineSettingsFactory.create()
        if (!session.vectorCallService.protocolChecker.supportVirtualRooms) {
            return mainRoom.createTimeline(eventId, settings)
        }
        val virtualRoomId = session.vectorCallService.userMapper.virtualRoomForNativeRoom(mainRoom.roomId)
        return if (virtualRoomId == null) {
            mainRoom.createTimeline(eventId, settings)
        } else {
            val virtualRoom = session.getRoom(virtualRoomId)!!
            MergedTimelines(
                    coroutineScope = coroutineScope,
                    mainTimeline = mainRoom.createTimeline(eventId, settings),
                    secondaryTimelineParams = MergedTimelines.SecondaryTimelineParams(
                            timeline = virtualRoom.createTimeline(null, settings),
                            shouldFilterTypes = true,
                            allowedTypes = secondaryTimelineAllowedTypes
                    )
            )
        }
    }
}
