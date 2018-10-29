package im.vector.matrix.android.internal.session.room.members

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.leftIfNull
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.sync.StateEventsChunkHandler
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class LoadRoomMembersRequest(private val roomAPI: RoomAPI,
                                      private val monarchy: Monarchy,
                                      private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                      private val stateEventsChunkHandler: StateEventsChunkHandler) {

    fun execute(roomId: String,
                streamToken: String?,
                excludeMembership: Membership? = null,
                callback: MatrixCallback<Boolean>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val responseOrFailure = execute(roomId, streamToken, excludeMembership)
            responseOrFailure.bimap({ callback.onFailure(it) }, { callback.onSuccess(true) })
        }
        return CancelableCoroutine(job)
    }

    //TODO : manage stream token (we have 404 on some rooms actually)
    private suspend fun execute(roomId: String,
                                streamToken: String?,
                                excludeMembership: Membership?) = withContext(coroutineDispatchers.io) {

        return@withContext executeRequest<RoomMembersResponse> {
            apiCall = roomAPI.getMembers(roomId, null, null, excludeMembership?.value)
        }.leftIfNull {
            Failure.Unknown(RuntimeException("RoomMembersResponse shouldn't be null"))
        }.flatMap { response ->
            try {
                insertInDb(response, roomId)
                Either.right(response)
            } catch (exception: Exception) {
                Either.Left(Failure.Unknown(exception))
            }
        }
    }

    private fun insertInDb(response: RoomMembersResponse, roomId: String) {
        monarchy.runTransactionSync { realm ->
            // We ignore all the already known members
            val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                             ?: throw IllegalStateException("You shouldn't use this method without a room")

            val roomMembers = RoomMembers(realm, roomId).getLoaded()
            val eventsToInsert = response.roomMemberEvents.filter { !roomMembers.containsKey(it.stateKey) }

            val chunk = stateEventsChunkHandler.handle(realm, roomId, eventsToInsert)
            if (!roomEntity.chunks.contains(chunk)) {
                roomEntity.chunks.add(chunk)
            }
            roomEntity.areAllMembersLoaded = true
        }
    }

}