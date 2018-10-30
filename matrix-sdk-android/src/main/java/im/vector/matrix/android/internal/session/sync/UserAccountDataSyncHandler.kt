package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataSync

class UserAccountDataSyncHandler(private val monarchy: Monarchy) {

    fun handle(accountData: UserAccountDataSync) {
        accountData.list.forEach {
            when (it) {
                is UserAccountDataDirectMessages -> handleDirectChatRooms(it)
                else                             -> return@forEach
            }
        }
    }

    private fun handleDirectChatRooms(directMessages: UserAccountDataDirectMessages) {
        val newDirectRoomIds = directMessages.content.values.flatten()
        monarchy.runTransactionSync { realm ->

            val oldDirectRooms = RoomSummaryEntity.where(realm).equalTo(RoomSummaryEntityFields.IS_DIRECT, true).findAll()
            oldDirectRooms.forEach { it.isDirect = false }

            newDirectRoomIds.forEach { roomId ->
                val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst()
                if (roomSummaryEntity != null) {
                    roomSummaryEntity.isDirect = true
                    realm.insertOrUpdate(roomSummaryEntity)
                }
            }
        }
    }
}