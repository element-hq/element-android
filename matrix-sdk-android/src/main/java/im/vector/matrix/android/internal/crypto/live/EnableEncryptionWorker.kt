/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto.live

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.members.RoomMembers
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject

internal class EnableEncryptionWorker(context: Context,
                                      workerParameters: WorkerParameters
) : Worker(context, workerParameters), MatrixKoinComponent {

    private val monarchy by inject<Monarchy>()
    private val cryptoManager by inject<CryptoManager>()
    private val loadRoomMembersTask by inject<LoadRoomMembersTask>()
    private val taskExecutor by inject<TaskExecutor>()

    @JsonClass(generateAdapter = true)
    internal class Params(
            val eventIds: List<String>
    )


    override fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                     ?: return Result.failure()


        val events = monarchy.fetchAllMappedSync(
                { EventEntity.where(it, params.eventIds) },
                { it.asDomain() }
        )

        events.forEach {
            val roomId = it.roomId!!

            val callback = object : MatrixCallback<Boolean> {
                override fun onSuccess(data: Boolean) {
                    super.onSuccess(data)

                }
            }

            loadRoomMembersTask
                    .configureWith(LoadRoomMembersTask.Params(roomId))
                    .executeOn(TaskThread.ENCRYPTION)
                    .dispatchTo(callback)
                    .executeBy(taskExecutor)

            var userIds: List<String> = emptyList()

            monarchy.doWithRealm { realm ->
                // Check whether the event content must be encrypted for the invited members.
                val encryptForInvitedMembers = cryptoManager.isEncryptionEnabledForInvitedUser()
                                               && cryptoManager.shouldEncryptForInvitedMembers(roomId)


                userIds = if (encryptForInvitedMembers) {
                    RoomMembers(realm, roomId).getActiveRoomMemberIds()
                } else {
                    RoomMembers(realm, roomId).getJoinedRoomMemberIds()
                }

            }

            cryptoManager.onRoomEncryptionEvent(it, userIds)
        }

        return Result.success()
    }


}