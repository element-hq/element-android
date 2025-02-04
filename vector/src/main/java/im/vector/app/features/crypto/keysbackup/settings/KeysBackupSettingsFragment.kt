/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentKeysBackupSettingsBinding
import im.vector.app.features.crypto.keysbackup.restore.KeysBackupRestoreActivity
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class KeysBackupSettingsFragment :
        VectorBaseFragment<FragmentKeysBackupSettingsBinding>(),
        KeysBackupSettingsRecyclerViewController.Listener {

    @Inject lateinit var keysBackupSettingsRecyclerViewController: KeysBackupSettingsRecyclerViewController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeysBackupSettingsBinding {
        return FragmentKeysBackupSettingsBinding.inflate(inflater, container, false)
    }

    private val viewModel: KeysBackupSettingsViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.keysBackupSettingsRecyclerView.configureWith(keysBackupSettingsRecyclerViewController)
        keysBackupSettingsRecyclerViewController.listener = this
    }

    override fun onDestroyView() {
        keysBackupSettingsRecyclerViewController.listener = null
        views.keysBackupSettingsRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        keysBackupSettingsRecyclerViewController.setData(state)
    }

    override fun didSelectSetupMessageRecovery() {
        viewModel.handle(KeyBackupSettingsAction.SetUpKeyBackup)
    }

    override fun didSelectRestoreMessageRecovery() {
        context?.let {
            startActivity(KeysBackupRestoreActivity.intent(it))
        }
    }

    override fun didSelectDeleteSetupMessageRecovery() {
        activity?.let {
            MaterialAlertDialogBuilder(it)
                    .setTitle(CommonStrings.keys_backup_settings_delete_confirm_title)
                    .setMessage(CommonStrings.keys_backup_settings_delete_confirm_message)
                    .setCancelable(false)
                    .setPositiveButton(CommonStrings.keys_backup_settings_delete_confirm_title) { _, _ ->
                        viewModel.handle(KeyBackupSettingsAction.DeleteKeyBackup)
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .setCancelable(true)
                    .show()
        }
    }

    override fun loadTrustData() {
        viewModel.handle(KeyBackupSettingsAction.GetKeyBackupTrust)
    }

    override fun loadKeysBackupState() {
        viewModel.handle(KeyBackupSettingsAction.Init)
    }
}
