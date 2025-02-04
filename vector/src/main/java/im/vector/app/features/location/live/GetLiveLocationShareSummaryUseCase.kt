/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import timber.log.Timber
import javax.inject.Inject

class GetLiveLocationShareSummaryUseCase @Inject constructor(
        private val session: Session,
) {

    suspend fun execute(roomId: String, eventId: String): Flow<LiveLocationShareAggregatedSummary?> = withContext(session.coroutineDispatchers.main) {
        Timber.d("getting flow for roomId=$roomId and eventId=$eventId")
        session.getRoom(roomId)
                ?.locationSharingService()
                ?.getLiveLocationShareSummary(eventId)
                ?.asFlow()
                ?.map { it.getOrNull() }
                ?: emptyFlow()
    }
}
