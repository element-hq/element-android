/*
 * Copyright (c) 2020 New Vector Ltd
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
