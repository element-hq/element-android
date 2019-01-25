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

import arrow.core.Try
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import timber.log.Timber

internal class SyncResponseHandler(private val roomSyncHandler: RoomSyncHandler,
                                   private val userAccountDataSyncHandler: UserAccountDataSyncHandler,
                                   private val groupSyncHandler: GroupSyncHandler) {

    fun handleResponse(syncResponse: SyncResponse, fromToken: String?, isCatchingUp: Boolean): Try<SyncResponse> {
        return Try {
            Timber.v("Handle sync response")
            if (syncResponse.rooms != null) {
                roomSyncHandler.handle(syncResponse.rooms)
            }
            if (syncResponse.groups != null) {
                groupSyncHandler.handle(syncResponse.groups)
            }
            if (syncResponse.accountData != null) {
                userAccountDataSyncHandler.handle(syncResponse.accountData)
            }
            syncResponse
        }
    }

}