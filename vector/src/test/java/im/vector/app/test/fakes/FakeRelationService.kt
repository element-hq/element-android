/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.model.relation.RelationService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Cancelable

class FakeRelationService : RelationService by mockk() {

    private val cancelable = mockk<Cancelable>()

    override fun editPoll(targetEvent: TimelineEvent, pollType: PollType, question: String, options: List<String>): Cancelable = cancelable
}
