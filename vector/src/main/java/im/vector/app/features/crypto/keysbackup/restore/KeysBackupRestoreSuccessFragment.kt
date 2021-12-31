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
package im.vector.app.features.crypto.keysbackup.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.LiveEvent
import im.vector.app.databinding.FragmentKeysBackupRestoreSuccessBinding
import javax.inject.Inject

class KeysBackupRestoreSuccessFragment @Inject constructor() : VectorBaseFragment<FragmentKeysBackupRestoreSuccessBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeysBackupRestoreSuccessBinding {
        return FragmentKeysBackupRestoreSuccessBinding.inflate(inflater, container, false)
    }

    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel = activityViewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)

        if (compareValues(sharedViewModel.importKeyResult?.totalNumberOfKeys, 0) > 0) {
            sharedViewModel.importKeyResult?.let {
                val part1 = resources.getQuantityString(R.plurals.keys_backup_restore_success_description_part1,
                        it.totalNumberOfKeys, it.totalNumberOfKeys)
                val part2 = resources.getQuantityString(R.plurals.keys_backup_restore_success_description_part2,
                        it.successfullyNumberOfImportedKeys, it.successfullyNumberOfImportedKeys)
                views.successDetailsText.text = String.format("%s\n%s", part1, part2)
            }
            // We don't put emoji in string xml as it will crash on old devices
            views.successText.text = context?.getString(R.string.keys_backup_restore_success_title, "🎉")
        } else {
            views.successText.text = context?.getString(R.string.keys_backup_restore_success_title_already_up_to_date)
            views.successDetailsText.isVisible = false
        }
        views.keysBackupSetupDoneButton.debouncedClicks { onDone() }
    }

    private fun onDone() {
        sharedViewModel.importRoomKeysFinishWithResult.value = LiveEvent(sharedViewModel.importKeyResult!!)
    }
}
