/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.R
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.sync.model.GroupsSyncResponse
import im.vector.matrix.android.internal.session.sync.model.InvitedGroupSync
import im.vector.matrix.sqldelight.session.Memberships
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

internal class GroupSyncHandler @Inject constructor(private val sessionDatabase: SessionDatabase) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, Any>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedGroupSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, Any>) : HandlingStrategy()
    }

    fun handle(
            roomsSyncResponse: GroupsSyncResponse,
            reporter: DefaultInitialSyncProgressService? = null
    ) {
        handleGroupSync(HandlingStrategy.JOINED(roomsSyncResponse.join), reporter)
        handleGroupSync(HandlingStrategy.INVITED(roomsSyncResponse.invite), reporter)
        handleGroupSync(HandlingStrategy.LEFT(roomsSyncResponse.leave), reporter)
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleGroupSync(handlingStrategy: HandlingStrategy, reporter: DefaultInitialSyncProgressService?) {
        when (handlingStrategy) {
            is HandlingStrategy.JOINED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_groups, 0.6f) {
                    sessionDatabase.groupQueries.insertOrReplaceGroup(it.key, Memberships.JOIN)
                }

            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_groups, 0.3f) {
                    sessionDatabase.groupQueries.insertOrReplaceGroup(it.key, Memberships.INVITE)
                }

            is HandlingStrategy.LEFT ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_groups, 0.1f) {
                    sessionDatabase.groupQueries.insertOrReplaceGroup(it.key, Memberships.LEAVE)
                }
        }
    }

}
