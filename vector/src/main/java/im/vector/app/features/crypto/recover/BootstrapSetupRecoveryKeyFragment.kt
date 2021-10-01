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
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapSetupRecoveryBinding
import javax.inject.Inject

class BootstrapSetupRecoveryKeyFragment @Inject constructor() :
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
        if (state.step is BootstrapStep.FirstForm) {
            if (state.step.keyBackUpExist) {
                // Display the set up action
                views.bootstrapSetupSecureSubmit.isVisible = true
                views.bootstrapSetupSecureUseSecurityKey.isVisible = false
                views.bootstrapSetupSecureUseSecurityPassphrase.isVisible = false
                views.bootstrapSetupSecureUseSecurityPassphraseSeparator.isVisible = false
            } else {
                if (state.step.reset) {
                    views.bootstrapSetupSecureText.text = getString(R.string.reset_secure_backup_title)
                    views.bootstrapSetupWarningTextView.isVisible = true
                } else {
                    views.bootstrapSetupSecureText.text = getString(R.string.bottom_sheet_setup_secure_backup_subtitle)
                    views.bootstrapSetupWarningTextView.isVisible = false
                }
                // Choose between create a passphrase or use a recovery key
                views.bootstrapSetupSecureSubmit.isVisible = false
                views.bootstrapSetupSecureUseSecurityKey.isVisible = true
                views.bootstrapSetupSecureUseSecurityPassphrase.isVisible = true
                views.bootstrapSetupSecureUseSecurityPassphraseSeparator.isVisible = true
            }
        }
    }
}
