package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.timeline.PaginationRequest
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import io.realm.Sort
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.Executors

data class DefaultRoom(
        override val roomId: String
) : Room, KoinComponent {

    private val paginationRequest by inject<PaginationRequest>()
    private val monarchy by inject<Monarchy>()
    private val boundaryCallback = TimelineBoundaryCallback(paginationRequest, roomId, monarchy, Executors.newSingleThreadExecutor())

    override fun liveTimeline(): LiveData<PagedList<Event>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            ChunkEntity.where(realm, roomId)
                    .findAll()
                    .last(null)
                    ?.let {
                        it.events.where().sort("originServerTs", Sort.DESCENDING)
                    }
        }
        val domainSourceFactory = realmDataSourceFactory.map { EventMapper.map(it) }

        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(10)
                .setPrefetchDistance(10)
                .build()

        val livePagedListBuilder = LivePagedListBuilder(domainSourceFactory, pagedListConfig).setBoundaryCallback(boundaryCallback)
        return monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)
    }


}