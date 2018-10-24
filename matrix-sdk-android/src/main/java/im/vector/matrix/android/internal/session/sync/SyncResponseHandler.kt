package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import timber.log.Timber

internal class SyncResponseHandler(private val roomSyncHandler: RoomSyncHandler) {

    fun handleResponse(syncResponse: SyncResponse?, fromToken: String?, isCatchingUp: Boolean) {
        if (syncResponse == null) {
            return
        }
        Timber.v("Handle sync response")

        if (syncResponse.rooms != null) {
            // joined rooms events
            roomSyncHandler.handleRoomSync(RoomSyncHandler.HandlingStrategy.JOINED(syncResponse.rooms.join))
            roomSyncHandler.handleRoomSync(RoomSyncHandler.HandlingStrategy.INVITED(syncResponse.rooms.invite))
            roomSyncHandler.handleRoomSync(RoomSyncHandler.HandlingStrategy.LEFT(syncResponse.rooms.leave))
        }

    }


}