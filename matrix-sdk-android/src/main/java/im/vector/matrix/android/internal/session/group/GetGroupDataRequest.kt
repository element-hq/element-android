package im.vector.matrix.android.internal.session.group

import arrow.core.Try
import arrow.core.fix
import arrow.instances.`try`.monad.monad
import arrow.typeclasses.binding
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.group.model.GroupRooms
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.kotlin.createObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GetGroupDataRequest(
        private val groupAPI: GroupAPI,
        private val monarchy: Monarchy,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    fun execute(groupId: String,
                callback: MatrixCallback<Boolean>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val groupOrFailure = execute(groupId)
            groupOrFailure.fold({ callback.onFailure(it) }, { callback.onSuccess(true) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(groupId: String) = withContext(coroutineDispatchers.io) {
        Try.monad().binding {
            val groupSummary = executeRequest<GroupSummaryResponse> {
                apiCall = groupAPI.getSummary(groupId)
            }.bind()

            val groupRooms = executeRequest<GroupRooms> {
                apiCall = groupAPI.getRooms(groupId)
            }.bind()
            insertInDb(groupSummary, groupRooms, groupId).bind()
        }.fix()
    }

    private fun insertInDb(groupSummary: GroupSummaryResponse, groupRooms: GroupRooms, groupId: String): Try<Unit> {
        return monarchy
                .tryTransactionSync { realm ->
                    val groupSummaryEntity = GroupSummaryEntity.where(realm, groupId).findFirst()
                                             ?: realm.createObject(groupId)

                    groupSummaryEntity.avatarUrl = groupSummary.profile?.avatarUrl ?: ""
                    val name = groupSummary.profile?.name
                    groupSummaryEntity.displayName = if (name.isNullOrEmpty()) groupId else name
                    groupSummaryEntity.shortDescription = groupSummary.profile?.shortDescription ?: ""

                    val roomIds = groupRooms.rooms.mapNotNull { it.roomId }
                    groupSummaryEntity.roomIds.clear()
                    groupSummaryEntity.roomIds.addAll(roomIds)
                }
    }


}