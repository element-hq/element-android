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
import android.view.View
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_bootstrap_setup_recovery.*
import javax.inject.Inject

class BootstrapSetupRecoveryKeyFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_bootstrap_setup_recovery

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Actions when a key backup exist
        bootstrapSetupSecureSubmit.clickableView.debouncedClicks {
            sharedViewModel.handle(BootstrapActions.StartKeyBackupMigration)
        }

        // Actions when there is no key backup
        bootstrapSetupSecureUseSecurityKey.clickableView.debouncedClicks {
            sharedViewModel.handle(BootstrapActions.Start(userWantsToEnterPassphrase = false))
        }
        bootstrapSetupSecureUseSecurityPassphrase.clickableView.debouncedClicks {
            sharedViewModel.handle(BootstrapActions.Start(userWantsToEnterPassphrase = true))
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        if (state.step is BootstrapStep.FirstForm) {
            if (state.step.keyBackUpExist) {
                // Display the set up action
                bootstrapSetupSecureSubmit.isVisible = true
                bootstrapSetupSecureUseSecurityKey.isVisible = false
                bootstrapSetupSecureUseSecurityPassphrase.isVisible = false
                bootstrapSetupSecureUseSecurityPassphraseSeparator.isVisible = false
            } else {
                if (state.step.reset) {
                    bootstrapSetupSecureText.text = getString(R.string.reset_secure_backup_title)
                    bootstrapSetupWarningTextView.isVisible = true
                } else {
                    bootstrapSetupSecureText.text = getString(R.string.bottom_sheet_setup_secure_backup_subtitle)
                    bootstrapSetupWarningTextView.isVisible = false
                }
                // Choose between create a passphrase or use a recovery key
                bootstrapSetupSecureSubmit.isVisible = false
                bootstrapSetupSecureUseSecurityKey.isVisible = true
                bootstrapSetupSecureUseSecurityPassphrase.isVisible = true
                bootstrapSetupSecureUseSecurityPassphraseSeparator.isVisible = true
            }
        }
    }
}
