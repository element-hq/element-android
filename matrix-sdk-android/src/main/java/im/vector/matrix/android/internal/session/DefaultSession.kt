package im.vector.matrix.android.internal.session

import android.arch.lifecycle.LiveData
import android.os.Looper
import android.support.annotation.MainThread
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.internal.auth.data.SessionParams
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.session.group.GroupModule
import im.vector.matrix.android.internal.session.room.RoomModule
import im.vector.matrix.android.internal.session.sync.SyncModule
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import org.koin.core.scope.Scope
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import org.koin.standalone.getKoin
import org.koin.standalone.inject


class DefaultSession(override val sessionParams: SessionParams) : Session, KoinComponent, RoomService {

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
        checkIsMainThread()
        assert(!isOpen)
        isOpen = true
        val sessionModule = SessionModule(sessionParams)
        val syncModule = SyncModule()
        val roomModule = RoomModule()
        val groupModule = GroupModule()
        StandAloneContext.loadKoinModules(listOf(sessionModule, syncModule, roomModule, groupModule))
        scope = getKoin().getOrCreateScope(SCOPE)
        liveEntityUpdaters.forEach { it.start() }
        syncThread.start()
    }


    @MainThread
    override fun close() {
        checkIsMainThread()
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

    override fun getAllRooms(): List<Room> {
        assert(isOpen)
        return roomService.getAllRooms()
    }

    override fun liveRooms(): LiveData<List<Room>> {
        assert(isOpen)
        return roomService.liveRooms()
    }

    override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
        assert(isOpen)
        return roomService.liveRoomSummaries()
    }

    override fun lastSelectedRoom(): RoomSummary? {
        assert(isOpen)
        return roomService.lastSelectedRoom()
    }

    override fun saveLastSelectedRoom(roomSummary: RoomSummary) {
        assert(isOpen)
        roomService.saveLastSelectedRoom(roomSummary)
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

    private fun checkIsMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("Should be called on main thread")
        }
    }

}