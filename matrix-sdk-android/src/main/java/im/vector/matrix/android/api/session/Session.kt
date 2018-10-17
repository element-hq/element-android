package im.vector.matrix.android.api.session

import android.support.annotation.MainThread
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.database.SessionRealmHolder
import im.vector.matrix.android.internal.session.sync.job.SyncThread

interface Session : RoomService {

    @MainThread
    fun open()

    @MainThread
    fun close()

    fun syncThread(): SyncThread

    // Visible for testing request directly. Will be deleted
    fun realmHolder(): SessionRealmHolder

}