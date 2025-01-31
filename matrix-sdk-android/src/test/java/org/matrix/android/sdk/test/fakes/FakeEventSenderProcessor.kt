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
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor

internal class FakeEventSenderProcessor : EventSenderProcessor by mockk() {

    fun givenPostEventReturns(event: Event, cancelable: Cancelable) {
        every { postEvent(event) } returns cancelable
    }

    fun givenPostRedaction(event: Event, reason: String?) {
        every { postRedaction(event, reason) } returns mockk()
    }
}
