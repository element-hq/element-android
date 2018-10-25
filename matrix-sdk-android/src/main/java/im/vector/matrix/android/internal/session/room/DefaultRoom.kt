package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.interceptor.EnrichedEventInterceptor
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.events.interceptor.MessageEventInterceptor
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersRequest
import im.vector.matrix.android.internal.session.room.timeline.PaginationRequest
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import io.realm.Sort
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.Executors

data class DefaultRoom(
        override val roomId: String,
        override val myMembership: MyMembership
) : Room, KoinComponent {

    private val paginationRequest by inject<PaginationRequest>()
    private val loadRoomMembersRequest by inject<LoadRoomMembersRequest>()
    private val syncTokenStore by inject<SyncTokenStore>()
    private val monarchy by inject<Monarchy>()

    private val boundaryCallback = TimelineBoundaryCallback(paginationRequest, roomId, monarchy, Executors.newSingleThreadExecutor())
    private val eventInterceptors = ArrayList<EnrichedEventInterceptor>()

    init {
        eventInterceptors.add(MessageEventInterceptor(monarchy))
    }

    override fun liveTimeline(): LiveData<PagedList<EnrichedEvent>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            ChunkEntity.where(realm, roomId)
                    .findAll()
                    .last(null)
                    ?.let {
                        it.events.where().sort("originServerTs", Sort.DESCENDING)
                    }
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
                .setPageSize(10)
                .setPrefetchDistance(10)
                .build()

        val livePagedListBuilder = LivePagedListBuilder(domainSourceFactory, pagedListConfig).setBoundaryCallback(boundaryCallback)
        return monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)
    }

    override fun getNumberOfJoinedMembers(): Int {
        val roomSummary = monarchy.fetchAllCopiedSync { realm -> RoomSummaryEntity.where(realm, roomId) }.firstOrNull()
        return roomSummary?.joinedMembersCount ?: 0
    }

    override fun loadRoomMembersIfNeeded(): Cancelable {
        return if (areAllMembersLoaded()) {
            object : Cancelable {}
        } else {
            val token = syncTokenStore.getLastToken()
            loadRoomMembersRequest.execute(roomId, token, Membership.LEAVE, object : MatrixCallback<Boolean> {})
        }
    }


    private fun areAllMembersLoaded(): Boolean {
        return monarchy
                       .fetchAllCopiedSync { RoomEntity.where(it, roomId) }
                       .firstOrNull()
                       ?.areAllMembersLoaded ?: false
    }


}