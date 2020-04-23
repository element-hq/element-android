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

package im.vector.matrix.android.internal.session.room.send

import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class LocalEchoUpdater @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                    private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                    private val taskExecutor: TaskExecutor) {

    fun updateSendState(eventId: String, sendState: SendState) = taskExecutor.executorScope.launch {
        Timber.v("Update local state of $eventId to ${sendState.name}")
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            sessionDatabase.eventQueries.updateSendState(
                    eventIds = listOf(eventId),
                    sendState = sendState.name,
                    ignoreSendStates = listOf(SendState.SENT.name, SendState.SYNCED.name)
            )
        }
    }


}
