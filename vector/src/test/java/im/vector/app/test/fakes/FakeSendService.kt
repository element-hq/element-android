/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.mockk
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.send.SendService
import org.matrix.android.sdk.api.util.Cancelable

class FakeSendService : SendService by mockk() {

    private val cancelable = mockk<Cancelable>()

    override fun sendPoll(pollType: PollType, question: String, options: List<String>, additionalContent: Content?): Cancelable = cancelable
}
