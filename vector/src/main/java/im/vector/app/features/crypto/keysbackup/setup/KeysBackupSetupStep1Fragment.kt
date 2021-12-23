/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.crypto.keysbackup.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.LiveEvent
import im.vector.app.databinding.FragmentKeysBackupSetupStep1Binding
import javax.inject.Inject

class KeysBackupSetupStep1Fragment @Inject constructor() : VectorBaseFragment<FragmentKeysBackupSetupStep1Binding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeysBackupSetupStep1Binding {
        return FragmentKeysBackupSetupStep1Binding.inflate(inflater, container, false)
    }

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activityViewModelProvider.get(KeysBackupSetupSharedViewModel::class.java)

        viewModel.showManualExport.observe(viewLifecycleOwner) {
            val showOption = it ?: false
            // Can't use isVisible because the kotlin compiler will crash with  Back-end (JVM) Internal error: wrong code generated
            views.keysBackupSetupStep1AdvancedOptionText.visibility = if (showOption) View.VISIBLE else View.GONE
            views.keysBackupSetupStep1ManualExportButton.visibility = if (showOption) View.VISIBLE else View.GONE
        }

        views.keysBackupSetupStep1Button.debouncedClicks { onButtonClick() }
        views.keysBackupSetupStep1ManualExportButton.debouncedClicks { onManualExportClick() }
    }

    private fun onButtonClick() {
        viewModel.navigateEvent.value = LiveEvent(KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_2)
    }

    private fun onManualExportClick() {
        viewModel.navigateEvent.value = LiveEvent(KeysBackupSetupSharedViewModel.NAVIGATE_MANUAL_EXPORT)
    }
}
