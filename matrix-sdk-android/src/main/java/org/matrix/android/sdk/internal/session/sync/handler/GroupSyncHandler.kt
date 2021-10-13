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

package org.matrix.android.sdk.internal.session.sync.handler

import io.realm.Realm
import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.sync.model.GroupsSyncResponse
import org.matrix.android.sdk.api.session.sync.model.InvitedGroupSync
import org.matrix.android.sdk.internal.database.model.GroupEntity
import org.matrix.android.sdk.internal.database.model.GroupSummaryEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.initsync.ProgressReporter
import org.matrix.android.sdk.internal.session.initsync.mapWithProgress
import javax.inject.Inject

internal class GroupSyncHandler @Inject constructor() {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, Any>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedGroupSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, Any>) : HandlingStrategy()
    }

    fun handle(realm: Realm,
               roomsSyncResponse: GroupsSyncResponse,
               reporter: ProgressReporter? = null) {
        handleGroupSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), reporter)
        handleGroupSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), reporter)
        handleGroupSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), reporter)
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleGroupSync(realm: Realm, handlingStrategy: HandlingStrategy, reporter: ProgressReporter?) {
        val groups = when (handlingStrategy) {
            is HandlingStrategy.JOINED  ->
                handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountGroups, 0.6f) {
                    handleJoinedGroup(realm, it.key)
                }

            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountGroups, 0.3f) {
                    handleInvitedGroup(realm, it.key)
                }

            is HandlingStrategy.LEFT    ->
                handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountGroups, 0.1f) {
                    handleLeftGroup(realm, it.key)
                }
        }
        realm.insertOrUpdate(groups)
    }

    private fun handleJoinedGroup(realm: Realm,
                                  groupId: String): GroupEntity {
        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        val groupSummaryEntity = GroupSummaryEntity.getOrCreate(realm, groupId)
        groupEntity.membership = Membership.JOIN
        groupSummaryEntity.membership = Membership.JOIN
        return groupEntity
    }

    private fun handleInvitedGroup(realm: Realm,
                                   groupId: String): GroupEntity {
        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        val groupSummaryEntity = GroupSummaryEntity.getOrCreate(realm, groupId)
        groupEntity.membership = Membership.INVITE
        groupSummaryEntity.membership = Membership.INVITE
        return groupEntity
    }

    private fun handleLeftGroup(realm: Realm,
                                groupId: String): GroupEntity {
        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        val groupSummaryEntity = GroupSummaryEntity.getOrCreate(realm, groupId)
        groupEntity.membership = Membership.LEAVE
        groupSummaryEntity.membership = Membership.LEAVE
        return groupEntity
    }
}
