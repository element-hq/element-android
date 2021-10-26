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

package im.vector.app.test.fakes

import im.vector.app.features.notifications.GroupedNotificationEvents
import im.vector.app.features.notifications.NotificationFactory
import im.vector.app.features.notifications.OneShotNotification
import im.vector.app.features.notifications.RoomNotification
import im.vector.app.features.notifications.SummaryNotification
import io.mockk.every
import io.mockk.mockk

class FakeNotificationFactory {

    val instance = mockk<NotificationFactory>()

    fun givenNotificationsFor(groupedEvents: GroupedNotificationEvents,
                              myUserId: String,
                              myUserDisplayName: String,
                              myUserAvatarUrl: String?,
                              useCompleteNotificationFormat: Boolean,
                              roomNotifications: List<RoomNotification>,
                              invitationNotifications: List<OneShotNotification>,
                              simpleNotifications: List<OneShotNotification>,
                              summaryNotification: SummaryNotification) {
        with(instance) {
            every { groupedEvents.roomEvents.toNotifications(myUserDisplayName, myUserAvatarUrl) } returns roomNotifications
            every { groupedEvents.invitationEvents.toNotifications(myUserId) } returns invitationNotifications
            every { groupedEvents.simpleEvents.toNotifications(myUserId) } returns simpleNotifications

            every {
                createSummaryNotification(
                        roomNotifications,
                        invitationNotifications,
                        simpleNotifications,
                        useCompleteNotificationFormat
                )
            } returns summaryNotification
        }
    }
}
