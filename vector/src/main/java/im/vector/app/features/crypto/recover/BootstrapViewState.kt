/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import com.nulabinc.zxcvbn.Strength
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.features.raw.wellknown.SecureBackupMethod
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo

data class BootstrapViewState(
        val setupMode: SetupMode,
        val step: BootstrapStep = BootstrapStep.CheckingMigration,
        val passphrase: String? = null,
        val migrationRecoveryKey: String? = null,
        val passphraseRepeat: String? = null,
        val crossSigningInitialization: Async<Unit> = Uninitialized,
        val passphraseStrength: Async<Strength> = Uninitialized,
        val passphraseConfirmMatch: Async<Unit> = Uninitialized,
        val recoveryKeyCreationInfo: SsssKeyCreationInfo? = null,
        val initializationWaitingViewData: WaitingViewData? = null,
        val recoverySaveFileProcess: Async<Unit> = Uninitialized,
        val isSecureBackupRequired: Boolean = false,
        val secureBackupMethod: SecureBackupMethod = SecureBackupMethod.KEY_OR_PASSPHRASE,
        val isRecoverySetup: Boolean = true
) : MavericksState {

    constructor(args: BootstrapBottomSheet.Args) : this(setupMode = args.setUpMode)
}
