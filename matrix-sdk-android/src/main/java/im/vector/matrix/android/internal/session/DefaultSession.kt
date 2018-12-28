package im.vector.matrix.android.internal.session

import android.arch.lifecycle.LiveData
import android.os.Looper
import android.support.annotation.MainThread
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.di.MatrixKoinHolder
import im.vector.matrix.android.internal.session.group.GroupModule
import im.vector.matrix.android.internal.session.room.RoomModule
import im.vector.matrix.android.internal.session.sync.SyncModule
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import org.koin.core.scope.Scope
import org.koin.standalone.inject


internal class DefaultSession(override val sessionParams: SessionParams) : Session, MatrixKoinComponent, RoomService {

    companion object {
        const val SCOPE: String = "session"
    }

    private lateinit var scope: Scope

    private val liveEntityUpdaters by inject<List<LiveEntityObserver>>()
    private val roomService by inject<RoomService>()
    private val groupService by inject<GroupService>()
    private val syncThread by inject<SyncThread>()
    private var isOpen = false

    @MainThread
    override fun open() {
        assertMainThread()
        assert(!isOpen)
        isOpen = true
        val sessionModule = SessionModule(sessionParams).definition
        val syncModule = SyncModule().definition
        val roomModule = RoomModule().definition
        val groupModule = GroupModule().definition
        MatrixKoinHolder.instance.loadModules(listOf(sessionModule, syncModule, roomModule, groupModule))
        scope = getKoin().getOrCreateScope(SCOPE)
        liveEntityUpdaters.forEach { it.start() }
        syncThread.start()
    }


    @MainThread
    override fun close() {
        assertMainThread()
        assert(isOpen)
        syncThread.kill()
        liveEntityUpdaters.forEach { it.dispose() }
        scope.close()
        isOpen = false
    }

    // ROOM SERVICE

    override fun getRoom(roomId: String): Room? {
        assert(isOpen)
        return roomService.getRoom(roomId)
    }


    override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
        assert(isOpen)
        return roomService.liveRoomSummaries()
    }

    // GROUP SERVICE

    override fun getGroup(groupId: String): Group? {
        assert(isOpen)
        return groupService.getGroup(groupId)
    }

    override fun liveGroupSummaries(): LiveData<List<GroupSummary>> {
        assert(isOpen)
        return groupService.liveGroupSummaries()
    }

    // Private methods *****************************************************************************

    private fun assertMainThread() {
        if (Looper.getMainLooper().thread !== Thread.currentThread()) {
            throw IllegalStateException("This method can only be called on the main thread!")
        }
    }

}