/*
 * Copyright 2019 New Vector Ltd
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

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.reporting.ReportingService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith

internal class DefaultReportingService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                   private val taskExecutor: TaskExecutor,
                                                                   private val reportContentTask: ReportContentTask
) : ReportingService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): ReportingService
    }

    override fun reportContent(eventId: String, score: Int, reason: String, callback: MatrixCallback<Unit>): Cancelable {
        val params = ReportContentTask.Params(roomId, eventId, score, reason)

        return reportContentTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
