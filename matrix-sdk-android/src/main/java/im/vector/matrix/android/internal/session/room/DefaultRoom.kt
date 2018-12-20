package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.session.room.TimelineHolder
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import org.koin.core.parameter.parametersOf
import org.koin.standalone.inject

internal data class DefaultRoom(
        override val roomId: String,
        override val myMembership: MyMembership
) : Room, MatrixKoinComponent {

    private val loadRoomMembersTask by inject<LoadRoomMembersTask>()
    private val syncTokenStore by inject<SyncTokenStore>()
    private val monarchy by inject<Monarchy>()
    private val timelineHolder by inject<TimelineHolder> { parametersOf(roomId) }
    private val sendService by inject<SendService> { parametersOf(roomId) }
    private val taskExecutor by inject<TaskExecutor>()

    override val roomSummary: LiveData<RoomSummary> by lazy {
        val liveData = monarchy
                .findAllMappedWithChanges(
                        { realm -> RoomSummaryEntity.where(realm, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME) },
                        { from -> from.asDomain() })

        Transformations.map(liveData) {
            it.first()
        }
    }

    override fun timeline(eventId: String?): LiveData<PagedList<EnrichedEvent>> {
        return timelineHolder.timeline(eventId)
    }

    override fun loadRoomMembersIfNeeded(): Cancelable {
        return if (areAllMembersLoaded()) {
            object : Cancelable {}
        } else {
            val token = syncTokenStore.getLastToken()
            val params = LoadRoomMembersTask.Params(roomId, token, Membership.LEAVE)
            loadRoomMembersTask.configureWith(params).executeBy(taskExecutor)
        }
    }

    private fun areAllMembersLoaded(): Boolean {
        return monarchy
                       .fetchAllCopiedSync { RoomEntity.where(it, roomId) }
                       .firstOrNull()
                       ?.areAllMembersLoaded ?: false
    }


    override fun sendTextMessage(text: String, callback: MatrixCallback<Event>): Cancelable {
        return sendService.sendTextMessage(text, callback)
    }

}