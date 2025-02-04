/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapSetupRecoveryBinding
import im.vector.app.features.raw.wellknown.SecureBackupMethod
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class BootstrapSetupRecoveryKeyFragment :
        VectorBaseFragment<FragmentBootstrapSetupRecoveryBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapSetupRecoveryBinding {
        return FragmentBootstrapSetupRecoveryBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Actions when a key backup exist
        views.bootstrapSetupSecureSubmit.views.bottomSheetActionClickableZone.debouncedClicks {
            sharedViewModel.handle(BootstrapActions.StartKeyBackupMigration)
        }

        // Actions when there is no key backup
        views.bootstrapSetupSecureUseSecurityKey.views.bottomSheetActionClickableZone.debouncedClicks {
            sharedViewModel.handle(BootstrapActions.Start(userWantsToEnterPassphrase = false))
        }
        views.bootstrapSetupSecureUseSecurityPassphrase.views.bottomSheetActionClickableZone.debouncedClicks {
            sharedViewModel.handle(BootstrapActions.Start(userWantsToEnterPassphrase = true))
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        val firstFormStep = state.step as? BootstrapStep.FirstForm ?: return@withState

        if (firstFormStep.keyBackUpExist) {
            renderStateWithExistingKeyBackup()
        } else {
            renderSetupHeader(needsReset = firstFormStep.reset)
            views.bootstrapSetupSecureSubmit.isVisible = false

            // Choose between create a passphrase or use a recovery key
            renderBackupMethodActions(firstFormStep.methods)
        }
        views.bootstrapSetupSecureText.giveAccessibilityFocusOnce()
    }

    private fun renderStateWithExistingKeyBackup() = with(views) {
        // Display the set up action
        bootstrapSetupSecureSubmit.isVisible = true
        // Disable creating backup / passphrase options
        bootstrapSetupSecureUseSecurityKey.isVisible = false
        bootstrapSetupSecureUseSecurityPassphrase.isVisible = false
        bootstrapSetupSecureUseSecurityPassphraseSeparator.isVisible = false
    }

    private fun renderSetupHeader(needsReset: Boolean) = with(views) {
        bootstrapSetupSecureText.text = if (needsReset) {
            getString(CommonStrings.reset_secure_backup_title)
        } else {
            getString(CommonStrings.bottom_sheet_setup_secure_backup_subtitle)
        }
        bootstrapSetupWarningTextView.isVisible = needsReset
    }

    private fun renderBackupMethodActions(method: SecureBackupMethod) = with(views) {
        bootstrapSetupSecureUseSecurityKey.isVisible = method.isKeyAvailable
        bootstrapSetupSecureUseSecurityPassphrase.isVisible = method.isPassphraseAvailable
        bootstrapSetupSecureUseSecurityPassphraseSeparator.isVisible = method.isPassphraseAvailable
    }
}
