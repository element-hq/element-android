/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.session.room.timeline

import org.matrix.android.sdk.internal.session.room.timeline.PaginationTask
import org.matrix.android.sdk.internal.session.room.timeline.TokenChunkEventPersistor
import javax.inject.Inject
import kotlin.random.Random

internal class FakePaginationTask @Inject constructor(private val tokenChunkEventPersistor: TokenChunkEventPersistor) : PaginationTask {

    override suspend fun execute(params: PaginationTask.Params): TokenChunkEventPersistor.Result {
        val fakeEvents = RoomDataHelper.createFakeListOfEvents(30)
        val tokenChunkEvent = FakeTokenChunkEvent(params.from, Random.nextLong().toString(), fakeEvents)
        return tokenChunkEventPersistor.insertInDb(tokenChunkEvent, params.roomId, params.direction)
    }
}
