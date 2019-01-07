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
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.timeline.TimelineData
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import org.koin.core.parameter.parametersOf
import org.koin.standalone.inject

internal data class DefaultRoom(
        override val roomId: String,
        override val myMembership: MyMembership
) : Room, MatrixKoinComponent {

    private val loadRoomMembersTask by inject<LoadRoomMembersTask>()
    private val monarchy by inject<Monarchy>()
    private val timelineService by inject<TimelineService> { parametersOf(roomId) }
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

    override fun timeline(eventId: String?): LiveData<TimelineData> {
        return timelineService.timeline(eventId)
    }

    override fun loadRoomMembersIfNeeded(): Cancelable {
        val params = LoadRoomMembersTask.Params(roomId, Membership.LEAVE)
        return loadRoomMembersTask.configureWith(params).executeBy(taskExecutor)
    }


    override fun sendTextMessage(text: String, callback: MatrixCallback<Event>): Cancelable {
        return sendService.sendTextMessage(text, callback)
    }

}