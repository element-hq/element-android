/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.state.StateService

class FakeStateService : StateService by mockk(relaxed = true) {

    fun givenGetStateEvents(stateKey: QueryStateEventValue, result: List<Event>) {
        every { getStateEvents(any(), stateKey) } returns result
    }

    fun givenGetStateEvent(event: Event?) {
        every { getStateEvent(any(), any()) } returns event
    }
}
