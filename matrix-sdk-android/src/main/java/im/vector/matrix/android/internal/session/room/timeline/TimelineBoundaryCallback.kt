package im.vector.matrix.android.internal.session.room.timeline

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.TimelineEvent
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
                .dispatchTo(object : MatrixCallback<Boolean> {
                    override fun onSuccess(data: Boolean) {
                        requestCallback.recordSuccess()
                    }

                    override fun onFailure(failure: Throwable) {
                        requestCallback.recordFailure(failure)
                    }
                })
                .executeBy(taskExecutor)
    }

}
