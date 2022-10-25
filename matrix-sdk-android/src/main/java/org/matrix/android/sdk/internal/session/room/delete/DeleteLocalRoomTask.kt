/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.delete

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereInRoom
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.delete.DeleteLocalRoomTask.Params
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

internal interface DeleteLocalRoomTask : Task<Params, Unit> {
    data class Params(val roomId: String)
}

internal class DefaultDeleteLocalRoomTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) : DeleteLocalRoomTask {

    override suspend fun execute(params: Params) {
        val roomId = params.roomId

        if (RoomLocalEcho.isLocalEchoId(roomId)) {
            monarchy.awaitTransaction { realm ->
                Timber.i("## DeleteLocalRoomTask - delete local room id $roomId")
                ReadReceiptsSummaryEntity.whereInRoom(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - ReadReceiptsSummaryEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                ReadReceiptEntity.whereRoomId(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - ReadReceiptEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                RoomMemberSummaryEntity.where(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - RoomMemberSummaryEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                CurrentStateEventEntity.whereRoomId(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - CurrentStateEventEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                EventEntity.whereRoomId(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - EventEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                TimelineEventEntity.whereRoomId(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - TimelineEventEntity - delete ${it.size} entries") }
                        ?.forEach { it.deleteOnCascade(true) }
                ChunkEntity.where(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - ChunkEntity - delete ${it.size} entries") }
                        ?.forEach { it.deleteOnCascade(deleteStateEvents = true, canDeleteRoot = true) }
                RoomSummaryEntity.where(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - RoomSummaryEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                RoomEntity.where(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - RoomEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
                LocalRoomSummaryEntity.where(realm, roomId = roomId).findAll()
                        ?.also { Timber.i("## DeleteLocalRoomTask - LocalRoomSummaryEntity - delete ${it.size} entries") }
                        ?.deleteAllFromRealm()
            }
        } else {
            Timber.i("## DeleteLocalRoomTask - Failed to remove room with id $roomId: not a local room")
        }
    }
}
