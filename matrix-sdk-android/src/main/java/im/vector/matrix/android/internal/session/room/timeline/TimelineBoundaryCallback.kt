package im.vector.matrix.android.internal.session.room.timeline

import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.session.room.model.PaginationDirection
import im.vector.matrix.android.internal.session.room.model.TokenChunkEvent
import im.vector.matrix.android.internal.util.PagingRequestHelper
import java.util.*
import java.util.concurrent.Executor

class TimelineBoundaryCallback(private val paginationRequest: PaginationRequest,
                               private val roomId: String,
                               private val monarchy: Monarchy,
                               ioExecutor: Executor
) : PagedList.BoundaryCallback<EnrichedEvent>() {

    private val helper = PagingRequestHelper(ioExecutor)

    override fun onZeroItemsLoaded() {
        // actually, it's not possible
    }

    override fun onItemAtEndLoaded(itemAtEnd: EnrichedEvent) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            monarchy.doWithRealm { realm ->
                if (itemAtEnd.root.eventId == null) {
                    return@doWithRealm
                }
                val chunkEntity = ChunkEntity.findAllIncludingEvents(realm, Collections.singletonList(itemAtEnd.root.eventId)).firstOrNull()
                paginationRequest.execute(roomId, chunkEntity?.prevToken, PaginationDirection.BACKWARDS, callback = createCallback(it))
            }
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: EnrichedEvent) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
            monarchy.doWithRealm { realm ->
                if (itemAtFront.root.eventId == null) {
                    return@doWithRealm
                }
                val chunkEntity = ChunkEntity.findAllIncludingEvents(realm, Collections.singletonList(itemAtFront.root.eventId)).firstOrNull()
                paginationRequest.execute(roomId, chunkEntity?.nextToken, PaginationDirection.FORWARDS, callback = createCallback(it))
            }
        }
    }

    private fun createCallback(pagingRequestCallback: PagingRequestHelper.Request.Callback) = object : MatrixCallback<TokenChunkEvent> {
        override fun onSuccess(data: TokenChunkEvent) {
            pagingRequestCallback.recordSuccess()
        }

        override fun onFailure(failure: Failure) {
            pagingRequestCallback.recordFailure(failure.toException())
        }
    }
}
