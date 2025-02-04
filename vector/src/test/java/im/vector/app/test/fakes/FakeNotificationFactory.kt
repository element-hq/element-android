/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    fun givenNotificationsFor(
            groupedEvents: GroupedNotificationEvents,
            myUserId: String,
            myUserDisplayName: String,
            myUserAvatarUrl: String?,
            useCompleteNotificationFormat: Boolean,
            roomNotifications: List<RoomNotification>,
            invitationNotifications: List<OneShotNotification>,
            simpleNotifications: List<OneShotNotification>,
            summaryNotification: SummaryNotification
    ) {
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
