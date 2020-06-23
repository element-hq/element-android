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

package im.vector.matrix.android.internal.session.room.state

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.content.UploadAvatarWorker
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val UPLOAD_AVATAR_WORK = "UPLOAD_AVATAR_WORK"

internal class DefaultStateService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               private val stateEventDataSource: StateEventDataSource,
                                                               private val taskExecutor: TaskExecutor,
                                                               private val sendStateTask: SendStateTask,
                                                               @SessionId private val sessionId: String,
                                                               private val workManagerProvider: WorkManagerProvider,
                                                               private val coroutineDispatchers: MatrixCoroutineDispatchers
) : StateService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): StateService
    }

    override fun getStateEvent(eventType: String, stateKey: QueryStringValue): Event? {
        return stateEventDataSource.getStateEvent(roomId, eventType, stateKey)
    }

    override fun getStateEventLive(eventType: String, stateKey: QueryStringValue): LiveData<Optional<Event>> {
        return stateEventDataSource.getStateEventLive(roomId, eventType, stateKey)
    }

    override fun getStateEvents(eventTypes: Set<String>, stateKey: QueryStringValue): List<Event> {
        return stateEventDataSource.getStateEvents(roomId, eventTypes, stateKey)
    }

    override fun getStateEventsLive(eventTypes: Set<String>, stateKey: QueryStringValue): LiveData<List<Event>> {
        return stateEventDataSource.getStateEventsLive(roomId, eventTypes, stateKey)
    }

    override fun sendStateEvent(
            eventType: String,
            stateKey: String?,
            body: JsonDict,
            callback: MatrixCallback<Unit>
    ): Cancelable {
        val params = SendStateTask.Params(
                roomId = roomId,
                stateKey = stateKey,
                eventType = eventType,
                body = body
        )
        return sendStateTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun updateTopic(topic: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_TOPIC,
                body = mapOf("topic" to topic),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateName(name: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_NAME,
                body = mapOf("name" to name),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateCanonicalAlias(alias: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_CANONICAL_ALIAS,
                body = mapOf("alias" to alias),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateHistoryReadability(readability: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                body = mapOf("history_visibility" to readability),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateAvatar(avatarUri: Uri, fileName: String, callback: MatrixCallback<Unit>): Cancelable {
        val cancelableBag = CancelableBag()
        val workerParams = UploadAvatarWorker.Params(sessionId, avatarUri, fileName)
        val workerData = WorkerParamsFactory.toData(workerParams)

        val uploadAvatarWork = workManagerProvider.matrixOneTimeWorkRequestBuilder<UploadAvatarWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(workerData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork("${roomId}_$UPLOAD_AVATAR_WORK", ExistingWorkPolicy.REPLACE, uploadAvatarWork)
                .enqueue()

        cancelableBag.add(CancelableWork(workManagerProvider.workManager, uploadAvatarWork.id))

        taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            workManagerProvider.workManager.getWorkInfoByIdLiveData(uploadAvatarWork.id)
                    .observeForever { info ->
                        if (info != null && info.state.isFinished) {
                            val result = WorkerParamsFactory.fromData<UploadAvatarWorker.OutputParams>(info.outputData)
                            cancelableBag.add(
                                    sendStateEvent(
                                            eventType = EventType.STATE_ROOM_AVATAR,
                                            body = mapOf("url" to result?.imageUrl!!),
                                            callback = callback,
                                            stateKey = null
                                    )
                            )
                        }
                    }
        }
        return cancelableBag
    }
}
