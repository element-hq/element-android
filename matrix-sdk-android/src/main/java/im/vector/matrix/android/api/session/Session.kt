package im.vector.matrix.android.api.session

import androidx.annotation.MainThread
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomService

interface Session : RoomService, GroupService {

    val sessionParams: SessionParams

    @MainThread
    fun open()

    @MainThread
    fun close()

    fun contentUrlResolver(): ContentUrlResolver

    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    // Not used at the moment
    interface Listener


}