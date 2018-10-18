package im.vector.matrix.android.internal.session.room.timeline

import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.session.room.model.TokenChunkEvent
import im.vector.matrix.android.internal.util.PagingRequestHelper
import java.util.*
import java.util.concurrent.Executor

class TimelineBoundaryCallback(private val paginationRequest: PaginationRequest,
                               private val roomId: String,
                               private val monarchy: Monarchy,
                               ioExecutor: Executor
) : PagedList.BoundaryCallback<Event>() {

    private val helper = PagingRequestHelper(ioExecutor)

    override fun onZeroItemsLoaded() {
        // actually, it's not possible
    }

    override fun onItemAtEndLoaded(itemAtEnd: Event) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            monarchy.doWithRealm { realm ->
                if (itemAtEnd.eventId == null) {
                    return@doWithRealm
                }
                val chunkEntity = ChunkEntity.findAllIncludingEvents(realm, Collections.singletonList(itemAtEnd.eventId)).firstOrNull()
                paginationRequest.execute(roomId, chunkEntity?.prevToken, "forward", callback = createCallback(it))
            }
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Event) {
        //Todo handle forward pagination
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
