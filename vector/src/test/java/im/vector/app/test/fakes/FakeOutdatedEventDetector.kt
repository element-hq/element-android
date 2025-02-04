/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.notifications.NotifiableEvent
import im.vector.app.features.notifications.OutdatedEventDetector
import io.mockk.every
import io.mockk.mockk

class FakeOutdatedEventDetector {
    val instance = mockk<OutdatedEventDetector>()

    fun givenEventIsOutOfDate(notifiableEvent: NotifiableEvent) {
        every { instance.isMessageOutdated(notifiableEvent) } returns true
    }

    fun givenEventIsInDate(notifiableEvent: NotifiableEvent) {
        every { instance.isMessageOutdated(notifiableEvent) } returns false
    }
}
