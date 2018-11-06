package im.vector.matrix.android.internal.session.sync

import arrow.core.Try
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import timber.log.Timber

internal class SyncResponseHandler(private val roomSyncHandler: RoomSyncHandler,
                                   private val userAccountDataSyncHandler: UserAccountDataSyncHandler,
                                   private val groupSyncHandler: GroupSyncHandler) {

    fun handleResponse(syncResponse: SyncResponse, fromToken: String?, isCatchingUp: Boolean): Try<SyncResponse> {
        return Try {
            Timber.v("Handle sync response")
            if (syncResponse.rooms != null) {
                roomSyncHandler.handle(syncResponse.rooms)
            }
            if (syncResponse.groups != null) {
                groupSyncHandler.handle(syncResponse.groups)
            }
            if (syncResponse.accountData != null) {
                userAccountDataSyncHandler.handle(syncResponse.accountData)
            }
            syncResponse
        }
    }

}