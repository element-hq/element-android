package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.LiveData
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.SessionRealmHolder
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.getAll
import im.vector.matrix.android.internal.database.query.getForId
import io.realm.Realm
import io.realm.RealmConfiguration

class DefaultRoomService(private val realmConfiguration: RealmConfiguration,
                         val mainThreadRealm: SessionRealmHolder)
    : RoomService {

    override fun getAllRooms(): List<Room> {
        val realm = Realm.getInstance(realmConfiguration)
        val rooms = RoomEntity.getAll(realm).findAll().map { DefaultRoom(it.roomId) }
        realm.close()
        return rooms
    }

    override fun getRoom(roomId: String): Room? {
        val realm = Realm.getInstance(realmConfiguration)
        val room = RoomEntity.getForId(realm, roomId)?.let { DefaultRoom(it.roomId) }
        realm.close()
        return room
    }

    override fun rooms(): LiveData<List<Room>> {
        val roomResults = RoomEntity.getAll(mainThreadRealm.instance).findAllAsync()
        return RealmLiveData(roomResults) { DefaultRoom(it.roomId) }
    }


}