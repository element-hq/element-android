/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.coEvery
import io.mockk.mockk
import org.matrix.android.sdk.api.session.events.EventService
import org.matrix.android.sdk.api.session.events.model.Event

class FakeEventService : EventService by mockk() {

    fun givenGetEventReturns(event: Event) {
        coEvery { getEvent(any(), any()) } returns event
    }
}
