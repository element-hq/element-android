package im.vector.matrix.android.api.session

import android.support.annotation.MainThread
import im.vector.matrix.android.internal.database.SessionRealmHolder
import im.vector.matrix.android.internal.events.sync.Synchronizer

interface Session {

    @MainThread
    fun open()

    fun synchronizer(): Synchronizer

    // Visible for testing request directly. Will be deleted
    fun realmHolder(): SessionRealmHolder

    @MainThread
    fun close()

}