package im.vector.matrix.android.api.session

import android.support.annotation.MainThread
import im.vector.matrix.android.internal.database.SessionRealmHolder
import im.vector.matrix.android.internal.events.sync.job.SyncThread

interface Session {

    @MainThread
    fun open()

    fun syncThread(): SyncThread

    // Visible for testing request directly. Will be deleted
    fun realmHolder(): SessionRealmHolder

    @MainThread
    fun close()

}