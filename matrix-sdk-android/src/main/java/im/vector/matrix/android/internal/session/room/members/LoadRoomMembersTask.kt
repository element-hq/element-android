package im.vector.matrix.android.internal.session.room.members

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.Task
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.tryTransactionSync

internal interface LoadRoomMembersTask : Task<LoadRoomMembersTask.Params, Boolean> {

    data class Params(
            val roomId: String,
            val streamToken: String?,
            val excludeMembership: Membership? = null
    )
}

internal class DefaultLoadRoomMembersTask(private val roomAPI: RoomAPI,
                                          private val monarchy: Monarchy
) : LoadRoomMembersTask {

    override fun execute(params: LoadRoomMembersTask.Params): Try<Boolean> {
        return executeRequest<RoomMembersResponse> {
            apiCall = roomAPI.getMembers(params.roomId, null, null, params.excludeMembership?.value)
        }.flatMap { response ->
            insertInDb(response, params.roomId)
        }.map { true }
    }

    private fun insertInDb(response: RoomMembersResponse, roomId: String): Try<RoomMembersResponse> {
        return monarchy
                .tryTransactionSync { realm ->
                    // We ignore all the already known members
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                                     ?: throw IllegalStateException("You shouldn't use this method without a room")

                    val roomMembers = RoomMembers(realm, roomId).getLoaded()
                    val eventsToInsert = response.roomMemberEvents.filter { !roomMembers.containsKey(it.stateKey) }

                    roomEntity.addStateEvents(eventsToInsert)
                    roomEntity.areAllMembersLoaded = true
                }
                .map { response }
    }

}