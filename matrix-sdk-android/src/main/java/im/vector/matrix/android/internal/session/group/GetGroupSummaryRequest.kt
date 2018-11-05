package im.vector.matrix.android.internal.session.group

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.leftIfNull
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import io.realm.kotlin.createObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GetGroupSummaryRequest(
        private val groupAPI: GroupAPI,
        private val monarchy: Monarchy,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    fun execute(groupId: String,
                callback: MatrixCallback<GroupSummaryResponse>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val groupOrFailure = execute(groupId)
            groupOrFailure.bimap({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(groupId: String) = withContext(coroutineDispatchers.io) {

        return@withContext executeRequest<GroupSummaryResponse> {
            apiCall = groupAPI.getSummary(groupId)
        }.leftIfNull {
            Failure.Unknown(RuntimeException("GroupSummary shouldn't be null"))
        }.flatMap { groupSummary ->
            try {
                insertInDb(groupSummary, groupId)
                Either.right(groupSummary)
            } catch (exception: Exception) {
                Either.Left(Failure.Unknown(exception))
            }
        }
    }

    private fun insertInDb(groupSummary: GroupSummaryResponse, groupId: String) {
        monarchy.runTransactionSync { realm ->
            val groupSummaryEntity = GroupSummaryEntity.where(realm, groupId).findFirst()
                                     ?: realm.createObject(groupId)

            groupSummaryEntity.avatarUrl = groupSummary.profile?.avatarUrl ?: ""
            val name = groupSummary.profile?.name
            groupSummaryEntity.displayName = if (name.isNullOrEmpty()) groupId else name
            groupSummaryEntity.shortDescription = groupSummary.profile?.shortDescription ?: ""


        }
    }


}