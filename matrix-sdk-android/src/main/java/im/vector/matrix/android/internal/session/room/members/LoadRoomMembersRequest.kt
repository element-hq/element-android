package im.vector.matrix.android.internal.session.room.members

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.tryTransactionSync
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class LoadRoomMembersRequest(private val roomAPI: RoomAPI,
                                      private val monarchy: Monarchy,
                                      private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    fun execute(roomId: String,
                streamToken: String?,
                excludeMembership: Membership? = null,
                callback: MatrixCallback<Boolean>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val responseOrFailure = execute(roomId, streamToken, excludeMembership)
            responseOrFailure.fold({ callback.onFailure(it) }, { callback.onSuccess(true) })
        }
        return CancelableCoroutine(job)
    }

    //TODO : manage stream token (we have 404 on some rooms actually)
    private suspend fun execute(roomId: String,
                                streamToken: String?,
                                excludeMembership: Membership?) = withContext(coroutineDispatchers.io) {

        executeRequest<RoomMembersResponse> {
            apiCall = roomAPI.getMembers(roomId, null, null, excludeMembership?.value)
        }.flatMap { response ->
            insertInDb(response, roomId)
        }
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