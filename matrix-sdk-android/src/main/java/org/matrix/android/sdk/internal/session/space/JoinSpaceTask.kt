/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.space

import io.realm.RealmConfiguration
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.SpaceSummaryEntity
import org.matrix.android.sdk.internal.database.model.SpaceSummaryEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.membership.joining.JoinRoomTask
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface JoinSpaceTask : Task<JoinSpaceTask.Params, Unit> {
    data class Params(
            val roomIdOrAlias: String,
            val reason: String?,
            val viaServers: List<String> = emptyList()
    )
}

internal class DefaultJoinSpaceTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val joinRoomTask: JoinRoomTask,
        @SessionDatabase
        private val realmConfiguration: RealmConfiguration,
        private val spaceSummaryDataSource: SpaceSummaryDataSource
) : JoinSpaceTask {

    override suspend fun execute(params: JoinSpaceTask.Params) {
        Timber.v("## Space: > Joining root space ${params.roomIdOrAlias} ...")
        joinRoomTask.execute(JoinRoomTask.Params(
                params.roomIdOrAlias,
                params.reason,
                params.viaServers
        ))
        Timber.v("## Space: < Joining root space done for ${params.roomIdOrAlias}")
        // we want to wait for sync result to check for auto join rooms

        Timber.v("## Space: > Wait for post joined sync ${params.roomIdOrAlias} ...")
        try {
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(2L)) { realm ->
                realm.where(SpaceSummaryEntity::class.java)
                        .apply {
                            if (params.roomIdOrAlias.startsWith("!")) {
                                equalTo(SpaceSummaryEntityFields.SPACE_ID, params.roomIdOrAlias)
                            } else {
                                equalTo(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.CANONICAL_ALIAS, params.roomIdOrAlias)
                            }
                        }
                        .equalTo(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.MEMBERSHIP_STR, Membership.JOIN.name)
            }
        } catch (exception: TimeoutCancellationException) {
            Timber.w("## Space: > Error created with timeout")
            throw CreateRoomFailure.CreatedWithTimeout
        }

        Timber.v("## Space: > Sync done ...")
        // after that i should have the children (? do i nead to paginate to get state)
        val summary = spaceSummaryDataSource.getSpaceSummary(params.roomIdOrAlias)
        Timber.v("## Space: Found space summary Name:[${summary?.roomSummary?.name}] children: ${summary?.children?.size}")
        summary?.children?.forEach {
            val childRoomSummary = it.roomSummary ?: return@forEach
            Timber.v("## Space: Processing child :[${childRoomSummary.roomId}] present: ${it.present} autoJoin:${it.autoJoin}")
            if (it.present && it.autoJoin) {
                // I should try to join as well
                if (childRoomSummary.roomType == RoomType.SPACE) {
                } else {
                    try {
                        Timber.v("## Space: Joining room child ${childRoomSummary.roomId}")
                        joinRoomTask.execute(JoinRoomTask.Params(
                                roomIdOrAlias = childRoomSummary.roomId,
                                reason = "Auto-join parent space",
                                viaServers = it.viaServers
                        ))
                    } catch (failure: Throwable) {
                        // todo keep track for partial success
                        Timber.e("## Space: Failed to join room child ${childRoomSummary.roomId}")
                    }
                }
            }
        }
    }
}

// try {
//    awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
//        realm.where(RoomEntity::class.java)
//                .equalTo(RoomEntityFields.ROOM_ID, roomId)
//    }
// } catch (exception: TimeoutCancellationException) {
//    throw CreateRoomFailure.CreatedWithTimeout
// }
