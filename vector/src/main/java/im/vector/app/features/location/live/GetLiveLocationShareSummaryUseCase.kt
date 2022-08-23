/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
