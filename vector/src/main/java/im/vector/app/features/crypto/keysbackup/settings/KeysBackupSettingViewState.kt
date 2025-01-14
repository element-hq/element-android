/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.keysbackup.settings

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrust
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult

data class KeysBackupSettingViewState(
        val keysBackupVersionTrust: Async<KeysBackupVersionTrust> = Uninitialized,
        val keysBackupState: KeysBackupState? = null,
        val keysBackupVersion: KeysVersionResult? = null,
        val remainingKeysToBackup: Int = 0,
        val deleteBackupRequest: Async<Unit> = Uninitialized
) :
        MavericksState
