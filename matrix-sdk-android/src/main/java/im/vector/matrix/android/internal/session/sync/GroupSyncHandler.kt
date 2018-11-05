package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupsSyncResponse
import im.vector.matrix.android.internal.legacy.rest.model.group.InvitedGroupSync
import io.realm.Realm


internal class GroupSyncHandler(private val monarchy: Monarchy) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, Any>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedGroupSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, Any>) : HandlingStrategy()
    }

    fun handle(roomsSyncResponse: GroupsSyncResponse) {
        monarchy.runTransactionSync { realm ->
            handleGroupSync(realm, GroupSyncHandler.HandlingStrategy.JOINED(roomsSyncResponse.join))
            handleGroupSync(realm, GroupSyncHandler.HandlingStrategy.INVITED(roomsSyncResponse.invite))
            handleGroupSync(realm, GroupSyncHandler.HandlingStrategy.LEFT(roomsSyncResponse.leave))
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleGroupSync(realm: Realm, handlingStrategy: HandlingStrategy) {
        val groups = when (handlingStrategy) {
            is HandlingStrategy.JOINED  -> handlingStrategy.data.map { handleJoinedGroup(realm, it.key) }
            is HandlingStrategy.INVITED -> handlingStrategy.data.map { handleInvitedGroup(realm, it.key) }
            is HandlingStrategy.LEFT    -> handlingStrategy.data.map { handleLeftGroup(realm, it.key) }
        }
        realm.insertOrUpdate(groups)
    }

    private fun handleJoinedGroup(realm: Realm,
                                  groupId: String): GroupEntity {

        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = MyMembership.JOINED
        return groupEntity
    }

    private fun handleInvitedGroup(realm: Realm,
                                   groupId: String): GroupEntity {

        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = MyMembership.INVITED
        return groupEntity

    }

    // TODO : handle it
    private fun handleLeftGroup(realm: Realm,
                                groupId: String): GroupEntity {

        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = MyMembership.LEFT
        return groupEntity
    }


}