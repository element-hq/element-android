/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import javax.inject.Inject

class InitSyncStepFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun format(initialSyncStep: InitialSyncStep): String {
        return stringProvider.getString(
                when (initialSyncStep) {
                    InitialSyncStep.ServerComputing -> R.string.initial_sync_start_server_computing
                    InitialSyncStep.Downloading -> R.string.initial_sync_start_downloading
                    InitialSyncStep.ImportingAccount -> R.string.initial_sync_start_importing_account
                    InitialSyncStep.ImportingAccountCrypto -> R.string.initial_sync_start_importing_account_crypto
                    InitialSyncStep.ImportingAccountRoom -> R.string.initial_sync_start_importing_account_rooms
                    InitialSyncStep.ImportingAccountData -> R.string.initial_sync_start_importing_account_data
                    InitialSyncStep.ImportingAccountJoinedRooms -> R.string.initial_sync_start_importing_account_joined_rooms
                    InitialSyncStep.ImportingAccountInvitedRooms -> R.string.initial_sync_start_importing_account_invited_rooms
                    InitialSyncStep.ImportingAccountLeftRooms -> R.string.initial_sync_start_importing_account_left_rooms
                }
        )
    }
}
