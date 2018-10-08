package im.vector.matrix.android.api.session

import im.vector.matrix.android.internal.events.sync.Synchronizer

interface Session {

    fun synchronizer(): Synchronizer

    fun close()

}