package im.vector.matrix.android.internal.session.room.timeline

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.interceptor.TimelineEventInterceptor
import im.vector.matrix.android.api.session.events.model.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineData
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomMemberExtractor
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.LiveDataUtils
import im.vector.matrix.android.internal.util.PagingRequestHelper
import im.vector.matrix.android.internal.util.tryTransactionAsync
import io.realm.Realm
import io.realm.RealmQuery

private const val PAGE_SIZE = 100
private const val PREFETCH_DISTANCE = 30
private const val EVENT_NOT_FOUND_INDEX = -1

internal class DefaultTimelineService(private val roomId: String,
                                      private val monarchy: Monarchy,
                                      private val taskExecutor: TaskExecutor,
                                      private val boundaryCallback: TimelineBoundaryCallback,
                                      private val contextOfEventTask: GetContextOfEventTask,
                                      private val roomMemberExtractor: RoomMemberExtractor
) : TimelineService {

    private val eventInterceptors = ArrayList<TimelineEventInterceptor>()

    override fun timeline(eventId: String?): LiveData<TimelineData> {
        clearUnlinkedEvents()
        val initialLoadKey = getInitialLoadKey(eventId)
        val realmDataSourceFactory = monarchy.createDataSourceFactory {
            buildDataSourceFactoryQuery(it, eventId)
        }
        val domainSourceFactory = realmDataSourceFactory
                .map { eventEntity ->
                    val roomMember = roomMemberExtractor.extractFrom(eventEntity)
                    TimelineEvent(eventEntity.asDomain(), eventEntity.localId, roomMember)
                }

        val pagedListConfig = buildPagedListConfig()

        val livePagedListBuilder = LivePagedListBuilder(domainSourceFactory, pagedListConfig)
                .setBoundaryCallback(boundaryCallback)
                .setInitialLoadKey(initialLoadKey)

        val eventsLiveData = monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)

        return LiveDataUtils.combine(eventsLiveData, boundaryCallback.status) { events, status ->
            val isLoadingForward = status.before == PagingRequestHelper.Status.RUNNING
            val isLoadingBackward = status.after == PagingRequestHelper.Status.RUNNING
            TimelineData(events, isLoadingForward, isLoadingBackward)
        }
    }

    // PRIVATE FUNCTIONS ***************************************************************************

    private fun getInitialLoadKey(eventId: String?): Int {
        var initialLoadKey = 0
        if (eventId != null) {
            val indexOfEvent = indexOfEvent(eventId)
            if (indexOfEvent == EVENT_NOT_FOUND_INDEX) {
                fetchEvent(eventId)
            } else {
                initialLoadKey = indexOfEvent
            }
        }
        return initialLoadKey
    }


    private fun fetchEvent(eventId: String) {
        val params = GetContextOfEventTask.Params(roomId, eventId)
        contextOfEventTask.configureWith(params).executeBy(taskExecutor)
    }

    private fun buildPagedListConfig(): PagedList.Config {
        return PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(PAGE_SIZE)
                .setInitialLoadSizeHint(2 * PAGE_SIZE)
                .setPrefetchDistance(PREFETCH_DISTANCE)
                .build()
    }

    private fun clearUnlinkedEvents() {
        monarchy.tryTransactionAsync { realm ->
            val unlinkedEvents = EventEntity
                    .where(realm, roomId = roomId)
                    .equalTo(EventEntityFields.IS_UNLINKED, true)
                    .findAll()
            unlinkedEvents.deleteAllFromRealm()
        }
    }

    private fun indexOfEvent(eventId: String): Int {
        var displayIndex = EVENT_NOT_FOUND_INDEX
        monarchy.doWithRealm {
            displayIndex = EventEntity.where(it, eventId = eventId).findFirst()?.displayIndex ?: EVENT_NOT_FOUND_INDEX
        }
        return displayIndex
    }

    private fun buildDataSourceFactoryQuery(realm: Realm, eventId: String?): RealmQuery<EventEntity> {
        val query = if (eventId == null) {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.LINKED_ONLY)
                    .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST}", true)
        } else {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.BOTH)
                    .`in`("${EventEntityFields.CHUNK}.${ChunkEntityFields.EVENTS.EVENT_ID}", arrayOf(eventId))
        }
        return query.sort(EventEntityFields.DISPLAY_INDEX)
    }


}