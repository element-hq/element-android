/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.version

import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.RoomUpgradeBody
import org.matrix.android.sdk.internal.task.Task
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface RoomVersionUpgradeTask : Task<RoomVersionUpgradeTask.Params, String> {
    data class Params(
            val roomId: String,
            val newVersion: String
    )
}

internal class DefaultRoomVersionUpgradeTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        @SessionDatabase
        private val realmConfiguration: RealmConfiguration
) : RoomVersionUpgradeTask {

    override suspend fun execute(params: RoomVersionUpgradeTask.Params): String {
        val replacementRoomId = executeRequest(globalErrorReceiver) {
            roomAPI.upgradeRoom(
                    roomId = params.roomId,
                    body = RoomUpgradeBody(params.newVersion)
            )
        }.replacementRoomId

        // Wait for room to come back from the sync (but it can maybe be in the DB if the sync response is received before)
        tryOrNull {
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomSummaryEntity::class.java)
                        .equalTo(RoomSummaryEntityFields.ROOM_ID, replacementRoomId)
                        .equalTo(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
            }
        }
        return replacementRoomId
    }
}
