/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.reporting

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.reporting.ReportingService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

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
