package im.vector.matrix.android.api.session

import android.support.annotation.MainThread
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.auth.data.SessionParams

interface Session : RoomService, GroupService {

    val sessionParams: SessionParams

    @MainThread
    fun open()

    @MainThread
    fun close()

}