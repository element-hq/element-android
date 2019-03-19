/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findIncludingEvent
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.PagingRequestHelper

internal class TimelineBoundaryCallback(private val roomId: String,
                                        private val taskExecutor: TaskExecutor,
                                        private val paginationTask: PaginationTask,
                                        private val monarchy: Monarchy,
                                        private val helper: PagingRequestHelper
) : PagedList.BoundaryCallback<TimelineEvent>() {

    var limit = 30

    val status = object : LiveData<PagingRequestHelper.StatusReport>() {

        init {
            value = PagingRequestHelper.StatusReport.createDefault()
        }

        val listener = PagingRequestHelper.Listener { postValue(it) }

        override fun onActive() {
            helper.addListener(listener)
        }

        override fun onInactive() {
            helper.removeListener(listener)
        }
    }

    override fun onZeroItemsLoaded() {
        // actually, it's not possible
    }

    override fun onItemAtEndLoaded(itemAtEnd: TimelineEvent) {
        val token = itemAtEnd.root.eventId?.let { getToken(it, PaginationDirection.BACKWARDS) }
                ?: return

        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            executePaginationTask(it, token, PaginationDirection.BACKWARDS)
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: TimelineEvent) {
        val token = itemAtFront.root.eventId?.let { getToken(it, PaginationDirection.FORWARDS) }
                ?: return

        helper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
            executePaginationTask(it, token, PaginationDirection.FORWARDS)
        }
    }

    private fun getToken(eventId: String, direction: PaginationDirection): String? {
        var token: String? = null
        monarchy.doWithRealm { realm ->
            val chunkEntity = ChunkEntity.findIncludingEvent(realm, eventId)
            token = if (direction == PaginationDirection.FORWARDS) chunkEntity?.nextToken else chunkEntity?.prevToken
        }
        return token
    }

    private fun executePaginationTask(requestCallback: PagingRequestHelper.Request.Callback,
                                      from: String,
                                      direction: PaginationDirection) {

        val params = PaginationTask.Params(roomId = roomId,
                from = from,
                direction = direction,
                limit = limit)

        paginationTask.configureWith(params)
                .enableRetry()
                .dispatchTo(object : MatrixCallback<TokenChunkEventPersistor.Result> {
                    override fun onSuccess(data: TokenChunkEventPersistor.Result) {
                        requestCallback.recordSuccess()
                    }

                    override fun onFailure(failure: Throwable) {
                        requestCallback.recordFailure(failure)
                    }
                })
                .executeBy(taskExecutor)
    }

}
