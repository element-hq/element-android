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

import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import javax.inject.Inject

class InitSyncStepFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun format(initialSyncStep: InitialSyncStep): String {
        return stringProvider.getString(
                when (initialSyncStep) {
                    InitialSyncStep.ServerComputing -> CommonStrings.initial_sync_start_server_computing
                    InitialSyncStep.Downloading -> CommonStrings.initial_sync_start_downloading
                    InitialSyncStep.ImportingAccount -> CommonStrings.initial_sync_start_importing_account
                    InitialSyncStep.ImportingAccountCrypto -> CommonStrings.initial_sync_start_importing_account_crypto
                    InitialSyncStep.ImportingAccountRoom -> CommonStrings.initial_sync_start_importing_account_rooms
                    InitialSyncStep.ImportingAccountData -> CommonStrings.initial_sync_start_importing_account_data
                    InitialSyncStep.ImportingAccountJoinedRooms -> CommonStrings.initial_sync_start_importing_account_joined_rooms
                    InitialSyncStep.ImportingAccountInvitedRooms -> CommonStrings.initial_sync_start_importing_account_invited_rooms
                    InitialSyncStep.ImportingAccountLeftRooms -> CommonStrings.initial_sync_start_importing_account_left_rooms
                }
        )
    }
}
