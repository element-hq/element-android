/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource

internal class FakeStateEventDataSource {

    val instance: StateEventDataSource = mockk()

    fun givenGetStateEventReturns(event: Event?) {
        every {
            instance.getStateEvent(
                    roomId = any(),
                    eventType = any(),
                    stateKey = any()
            )
        } returns event
    }

    fun verifyGetStateEvent(roomId: String, eventType: String, stateKey: QueryStateEventValue) {
        verify {
            instance.getStateEvent(
                    roomId = roomId,
                    eventType = eventType,
                    stateKey = stateKey
            )
        }
    }
}
