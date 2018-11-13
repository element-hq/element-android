package im.vector.matrix.android.internal.session.room.timeline

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.interceptor.EnrichedEventInterceptor
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.room.TimelineHolder
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.session.events.interceptor.MessageEventInterceptor

private const val PAGE_SIZE = 30

internal class DefaultTimelineHolder(private val roomId: String,
                                     private val monarchy: Monarchy,
                                     private val boundaryCallback: TimelineBoundaryCallback
) : TimelineHolder {

    private val eventInterceptors = ArrayList<EnrichedEventInterceptor>()

    init {
        boundaryCallback.limit = PAGE_SIZE
        eventInterceptors.add(MessageEventInterceptor(monarchy))
    }

    override fun liveTimeline(): LiveData<PagedList<EnrichedEvent>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            ChunkEntity.findLastLiveChunkFromRoom(realm, roomId = roomId)
                    ?.events
                    ?.where()
        }
        val domainSourceFactory = realmDataSourceFactory
                .map { it.asDomain() }
                .map { event ->

                    val enrichedEvent = EnrichedEvent(event)
                    eventInterceptors
                            .filter {
                                it.canEnrich(enrichedEvent)
                            }.forEach {
                                it.enrich(roomId, enrichedEvent)
                            }
                    enrichedEvent
                }

        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(PAGE_SIZE)
                .setInitialLoadSizeHint(PAGE_SIZE)
                .setPrefetchDistance(20)
                .build()

        val livePagedListBuilder = LivePagedListBuilder(domainSourceFactory, pagedListConfig).setBoundaryCallback(boundaryCallback)
        return monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)
    }
}