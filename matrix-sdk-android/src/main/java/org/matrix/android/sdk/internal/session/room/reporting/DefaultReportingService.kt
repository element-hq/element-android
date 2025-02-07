/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.reporting

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.room.reporting.ReportingService

internal class DefaultReportingService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val reportContentTask: ReportContentTask,
        private val reportRoomTask: ReportRoomTask,
) : ReportingService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultReportingService
    }

    override suspend fun reportContent(eventId: String, score: Int, reason: String) {
        val params = ReportContentTask.Params(roomId, eventId, score, reason)
        reportContentTask.execute(params)
    }

    override suspend fun reportRoom(reason: String) {
        val params = ReportRoomTask.Params(roomId, reason)
        reportRoomTask.execute(params)
    }
}
