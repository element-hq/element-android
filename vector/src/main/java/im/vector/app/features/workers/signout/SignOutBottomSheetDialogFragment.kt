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

package im.vector.app.features.workers.signout

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetLogoutAndBackupBinding
import im.vector.app.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.recover.SetupMode

import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import javax.inject.Inject

// TODO this needs to be refactored to current standard and remove legacy
class SignOutBottomSheetDialogFragment :
        VectorBaseBottomSheetDialogFragment<BottomSheetLogoutAndBackupBinding>(),
        SignoutCheckViewModel.Factory {

    var onSignOut: Runnable? = null

    companion object {
        fun newInstance() = SignOutBottomSheetDialogFragment()
    }

    init {
        isCancelable = true
    }

    @Inject
    lateinit var viewModelFactory: SignoutCheckViewModel.Factory

    override fun create(initialState: SignoutCheckViewState): SignoutCheckViewModel {
        return viewModelFactory.create(initialState)
    }

    private val viewModel: SignoutCheckViewModel by fragmentViewModel(SignoutCheckViewModel::class)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRemoteStateIfNeeded()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.setupRecoveryButton.action = {
            BootstrapBottomSheet.show(parentFragmentManager, SetupMode.NORMAL)
        }

        views.exitAnywayButton.action = {
            context?.let {
                MaterialAlertDialogBuilder(it)
                        .setTitle(R.string.are_you_sure)
                        .setMessage(R.string.sign_out_bottom_sheet_will_lose_secure_messages)
                        .setPositiveButton(R.string.backup, null)
                        .setNegativeButton(R.string.action_sign_out) { _, _ ->
                            onSignOut?.run()
                        }
                        .show()
            }
        }

        views.signOutButton.action = {
            onSignOut?.run()
        }

        views.exportManuallyButton.action = {
            withState(viewModel) { state ->
                queryExportKeys(state.userId, manualExportKeysActivityResultLauncher)
            }
        }

        views.setupMegolmBackupButton.action = {
            setupBackupActivityResultLauncher.launch(KeysBackupSetupActivity.intent(requireContext(), true))
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        views.signoutExportingLoading.isVisible = false
        if (state.crossSigningSetupAllKeysKnown && !state.backupIsSetup) {
            views.bottomSheetSignoutWarningText.text = getString(R.string.sign_out_bottom_sheet_warning_no_backup)
            views.backingUpStatusGroup.isVisible = false
            // we should show option to setup 4S
            views.setupRecoveryButton.isVisible = true
            views.setupMegolmBackupButton.isVisible = false
            // We let the option to ignore and quit
            views.exportManuallyButton.isVisible = true
            views.exitAnywayButton.isVisible = true
            views.signOutButton.isVisible = false
        } else if (state.keysBackupState == KeysBackupState.Unknown || state.keysBackupState == KeysBackupState.Disabled) {
            views.bottomSheetSignoutWarningText.text = getString(R.string.sign_out_bottom_sheet_warning_no_backup)
            views.backingUpStatusGroup.isVisible = false
            // no key backup and cannot setup full 4S
            // we propose to setup
            // we should show option to setup 4S
            views.setupRecoveryButton.isVisible = false
            views.setupMegolmBackupButton.isVisible = true
            // We let the option to ignore and quit
            views.exportManuallyButton.isVisible = true
            views.exitAnywayButton.isVisible = true
            views.signOutButton.isVisible = false
        } else {
            // so keybackup is setup
            // You should wait until all are uploaded
            views.setupRecoveryButton.isVisible = false

            when (state.keysBackupState) {
                KeysBackupState.ReadyToBackUp -> {
                    views.bottomSheetSignoutWarningText.text = getString(R.string.action_sign_out_confirmation_simple)

                    // Ok all keys are backedUp
                    views.backingUpStatusGroup.isVisible = true
                    views.backupProgress.isVisible = false
                    views.backupCompleteImage.isVisible = true
                    views.backupStatusText.text = getString(R.string.keys_backup_info_keys_all_backup_up)

                    views.setupMegolmBackupButton.isVisible = false
                    views.exportManuallyButton.isVisible = false
                    views.exitAnywayButton.isVisible = false
                    // You can signout
                    views.signOutButton.isVisible = true
                }
                KeysBackupState.WillBackUp,
                KeysBackupState.BackingUp     -> {
                    views.bottomSheetSignoutWarningText.text = getString(R.string.sign_out_bottom_sheet_warning_backing_up)

                    // save in progress
                    views.backingUpStatusGroup.isVisible = true
                    views.backupProgress.isVisible = true
                    views.backupCompleteImage.isVisible = false
                    views.backupStatusText.text = getString(R.string.sign_out_bottom_sheet_backing_up_keys)

                    views.setupMegolmBackupButton.isVisible = false
                    views.exportManuallyButton.isVisible = false
                    views.exitAnywayButton.isVisible = true
                    views.signOutButton.isVisible = false
                }
                KeysBackupState.NotTrusted    -> {
                    views.bottomSheetSignoutWarningText.text = getString(R.string.sign_out_bottom_sheet_warning_backup_not_active)
                    // It's not trusted and we know there are unsaved keys..
                    views.backingUpStatusGroup.isVisible = false

                    // option to enter pass/key
                    views.setupMegolmBackupButton.isVisible = true
                    views.exportManuallyButton.isVisible = true
                    views.exitAnywayButton.isVisible = true
                    views.signOutButton.isVisible = false
                }
                else                          -> {
                    // mmm.. strange state

                    views.exitAnywayButton.isVisible = true
                }
            }
        }

        // final call if keys have been exported
        when (state.hasBeenExportedToFile) {
            is Loading -> {
                views.signoutExportingLoading.isVisible = true
                views.backingUpStatusGroup.isVisible = false

                views.setupRecoveryButton.isVisible = false
                views.setupMegolmBackupButton.isVisible = false
                views.exportManuallyButton.isVisible = false
                views.exitAnywayButton.isVisible = true
                views.signOutButton.isVisible = false
            }
            is Success -> {
                if (state.hasBeenExportedToFile.invoke()) {
                    views.bottomSheetSignoutWarningText.text = getString(R.string.action_sign_out_confirmation_simple)
                    views.backingUpStatusGroup.isVisible = false

                    views.setupRecoveryButton.isVisible = false
                    views.setupMegolmBackupButton.isVisible = false
                    views.exportManuallyButton.isVisible = false
                    views.exitAnywayButton.isVisible = false
                    views.signOutButton.isVisible = true
                }
            }
            else       -> {
            }
        }
        super.invalidate()
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetLogoutAndBackupBinding {
        return BottomSheetLogoutAndBackupBinding.inflate(inflater, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // We want to force the bottom sheet initial state to expanded
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            bottomSheetDialog.setOnShowListener { dialog ->
                val d = dialog as BottomSheetDialog
                (d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout)?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        return dialog
    }

    private val manualExportKeysActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data
            if (uri != null) {
                activity?.let { activity ->
                    ExportKeysDialog().show(activity, object : ExportKeysDialog.ExportKeyDialogListener {
                        override fun onPassphrase(passphrase: String) {
                            viewModel.handle(SignoutCheckViewModel.Actions.ExportKeys(passphrase, uri))
                        }
                    })
                }
            }
        }
    }

    private val setupBackupActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data?.getBooleanExtra(KeysBackupSetupActivity.MANUAL_EXPORT, false) == true) {
                viewModel.handle(SignoutCheckViewModel.Actions.KeySuccessfullyManuallyExported)
            }
        }
    }
}
