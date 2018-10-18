package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.timeline.PaginationRequest
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.Executors

data class DefaultRoom(
        override val roomId: String
) : Room, KoinComponent {

    private val paginationRequest by inject<PaginationRequest>()
    private val monarchy by inject<Monarchy>()
    private val boundaryCallback = TimelineBoundaryCallback(paginationRequest, roomId, monarchy, Executors.newSingleThreadExecutor())

    fun events(): LiveData<PagedList<Event>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            val lastChunk = ChunkEntity.where(realm, roomId).findAll().last()
            EventEntity.where(realm, lastChunk)
        }
        val domainSourceFactory = realmDataSourceFactory.map { EventMapper.map(it) }
        val livePagedListBuilder = LivePagedListBuilder(domainSourceFactory, 20).setBoundaryCallback(boundaryCallback)
        return monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)
    }


}