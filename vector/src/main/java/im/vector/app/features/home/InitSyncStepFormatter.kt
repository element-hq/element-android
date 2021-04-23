/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import javax.inject.Inject

class InitSyncStepFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun format(initSyncStep: InitSyncStep): String {
        return stringProvider.getString(
                when (initSyncStep) {
                    InitSyncStep.ServerComputing              -> R.string.initial_sync_start_server_computing
                    InitSyncStep.Downloading                  -> R.string.initial_sync_start_downloading
                    InitSyncStep.ImportingAccount             -> R.string.initial_sync_start_importing_account
                    InitSyncStep.ImportingAccountCrypto       -> R.string.initial_sync_start_importing_account_crypto
                    InitSyncStep.ImportingAccountRoom         -> R.string.initial_sync_start_importing_account_rooms
                    InitSyncStep.ImportingAccountGroups       -> R.string.initial_sync_start_importing_account_groups
                    InitSyncStep.ImportingAccountData         -> R.string.initial_sync_start_importing_account_data
                    InitSyncStep.ImportingAccountJoinedRooms  -> R.string.initial_sync_start_importing_account_joined_rooms
                    InitSyncStep.ImportingAccountInvitedRooms -> R.string.initial_sync_start_importing_account_invited_rooms
                    InitSyncStep.ImportingAccountLeftRooms    -> R.string.initial_sync_start_importing_account_left_rooms
                }
        )
    }
}
