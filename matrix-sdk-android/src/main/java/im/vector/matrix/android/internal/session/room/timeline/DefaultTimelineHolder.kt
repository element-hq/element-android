package im.vector.matrix.android.internal.session.room.timeline

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.interceptor.EnrichedEventInterceptor
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.room.TimelineHolder
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.events.interceptor.MessageEventInterceptor
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.Realm
import io.realm.RealmQuery

private const val PAGE_SIZE = 30

internal class DefaultTimelineHolder(private val roomId: String,
                                     private val monarchy: Monarchy,
                                     private val taskExecutor: TaskExecutor,
                                     private val boundaryCallback: TimelineBoundaryCallback,
                                     private val contextOfEventTask: GetContextOfEventTask
) : TimelineHolder {

    private val eventInterceptors = ArrayList<EnrichedEventInterceptor>()

    init {
        eventInterceptors.add(MessageEventInterceptor(monarchy, roomId))
    }

    override fun timeline(eventId: String?): LiveData<PagedList<EnrichedEvent>> {
        clearUnlinkedEvents()
        if (eventId != null) {
            fetchEventIfNeeded(eventId)
        }
        val realmDataSourceFactory = monarchy.createDataSourceFactory {
            buildDataSourceFactoryQuery(it, eventId)
        }
        val domainSourceFactory = realmDataSourceFactory
                .map { it.asDomain() }
                .map { event ->

                    val enrichedEvent = EnrichedEvent(event)
                    eventInterceptors
                            .filter {
                                it.canEnrich(enrichedEvent)
                            }.forEach {
                                it.enrich(enrichedEvent)
                            }
                    enrichedEvent
                }

        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(PAGE_SIZE)
                .build()

        val livePagedListBuilder = LivePagedListBuilder(domainSourceFactory, pagedListConfig).setBoundaryCallback(boundaryCallback)
        return monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)
    }

    private fun clearUnlinkedEvents() {
        monarchy.tryTransactionSync { realm ->
            val unlinkedEvents = EventEntity
                    .where(realm, roomId = roomId)
                    .equalTo(EventEntityFields.IS_UNLINKED, true)
                    .findAll()
            unlinkedEvents.deleteAllFromRealm()
        }
    }

    private fun fetchEventIfNeeded(eventId: String) {
        if (!isEventPersisted(eventId)) {
            val params = GetContextOfEventTask.Params(roomId, eventId)
            contextOfEventTask.configureWith(params).executeBy(taskExecutor)
        }
    }

    private fun isEventPersisted(eventId: String): Boolean {
        var isEventPersisted = false
        monarchy.doWithRealm {
            isEventPersisted = EventEntity.where(it, eventId = eventId).findFirst() != null
        }
        return isEventPersisted
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