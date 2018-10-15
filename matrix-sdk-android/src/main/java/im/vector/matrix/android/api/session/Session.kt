package im.vector.matrix.android.api.session

import im.vector.matrix.android.internal.database.RealmInstanceHolder
import im.vector.matrix.android.internal.events.sync.Synchronizer

interface Session {

    fun synchronizer(): Synchronizer

    // Visible for testing request directly. Will be deleted
    fun realmInstanceHolder(): RealmInstanceHolder

    fun close()

}