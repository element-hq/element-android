/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.read

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.query.isMarkedUnread
import org.matrix.android.sdk.internal.session.sync.RoomMarkedUnreadHandler
import org.matrix.android.sdk.internal.session.user.accountdata.AccountDataAPI
import timber.log.Timber
import javax.inject.Inject

internal interface SetMarkedUnreadTask : Task<SetMarkedUnreadTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val markedUnread: Boolean,
            val markedUnreadContent: MarkedUnreadContent = MarkedUnreadContent(markedUnread)
    )
}

internal class DefaultSetMarkedUnreadTask @Inject constructor(
        private val accountDataApi: AccountDataAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val roomMarkedUnreadHandler: RoomMarkedUnreadHandler,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SetMarkedUnreadTask {

    override suspend fun execute(params: SetMarkedUnreadTask.Params) {
        Timber.v("Execute set marked unread with params: $params")

        if (isMarkedUnread(monarchy.realmConfiguration, params.roomId) != params.markedUnread) {
            updateDatabase(params.roomId, params.markedUnread)
            executeRequest<Unit>(globalErrorReceiver) {
                isRetryable = true
                apiCall = accountDataApi.setRoomAccountData(userId, params.roomId, EventType.MARKED_UNREAD, params.markedUnreadContent)
            }
        }
    }

    private suspend fun updateDatabase(roomId: String, markedUnread: Boolean) {
        monarchy.awaitTransaction { realm ->
            roomMarkedUnreadHandler.handle(realm, roomId, MarkedUnreadContent(markedUnread))
            val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                    ?: return@awaitTransaction
            roomSummary.markedUnread = markedUnread
        }
    }
}
