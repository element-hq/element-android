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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import butterknife.BindView
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.recover.SetupMode
import kotlinx.android.synthetic.main.bottom_sheet_logout_and_backup.*
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import timber.log.Timber
import javax.inject.Inject

// TODO this needs to be refactored to current standard and remove legacy
class SignOutBottomSheetDialogFragment : VectorBaseBottomSheetDialogFragment(), SignoutCheckViewModel.Factory {

    @BindView(R.id.bottom_sheet_signout_warning_text)
    lateinit var sheetTitle: TextView

    @BindView(R.id.bottom_sheet_signout_backingup_status_group)
    lateinit var backingUpStatusGroup: ViewGroup

    @BindView(R.id.bottom_sheet_signout_icon_progress_bar)
    lateinit var backupProgress: ProgressBar

    @BindView(R.id.bottom_sheet_signout_icon)
    lateinit var backupCompleteImage: ImageView

    @BindView(R.id.bottom_sheet_backup_status_text)
    lateinit var backupStatusTex: TextView

    @BindView(R.id.signoutExportingLoading)
    lateinit var signoutExportingLoading: View

    @BindView(R.id.root_layout)
    lateinit var rootLayout: ViewGroup

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

        setupRecoveryButton.action = {
            BootstrapBottomSheet.show(parentFragmentManager, SetupMode.NORMAL)
        }

        exitAnywayButton.action = {
            context?.let {
                AlertDialog.Builder(it)
                        .setTitle(R.string.are_you_sure)
                        .setMessage(R.string.sign_out_bottom_sheet_will_lose_secure_messages)
                        .setPositiveButton(R.string.backup, null)
                        .setNegativeButton(R.string.action_sign_out) { _, _ ->
                            onSignOut?.run()
                        }
                        .show()
            }
        }

        signOutButton.action = {
            onSignOut?.run()
        }

        exportManuallyButton.action = {
            withState(viewModel) { state ->
                queryExportKeys(state.userId, manualExportKeysActivityResultLauncher)
            }
        }

        setupMegolmBackupButton.action = {
            setupBackupActivityResultLauncher.launch(KeysBackupSetupActivity.intent(requireContext(), true))
        }

        viewModel.observeViewEvents {
            when (it) {
                is SignoutCheckViewModel.ViewEvents.ExportKeys -> {
                    it.exporter
                            .export(requireContext(),
                                    it.passphrase,
                                    it.uri,
                                    object : MatrixCallback<Boolean> {
                                        override fun onSuccess(data: Boolean) {
                                            if (data) {
                                                viewModel.handle(SignoutCheckViewModel.Actions.KeySuccessfullyManuallyExported)
                                            } else {
                                                viewModel.handle(SignoutCheckViewModel.Actions.KeyExportFailed)
                                            }
                                        }

                                        override fun onFailure(failure: Throwable) {
                                            Timber.e("## Failed to export manually keys ${failure.localizedMessage}")
                                            viewModel.handle(SignoutCheckViewModel.Actions.KeyExportFailed)
                                        }
                                    })
                }
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        signoutExportingLoading.isVisible = false
        if (state.crossSigningSetupAllKeysKnown && !state.backupIsSetup) {
            sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_no_backup)
            backingUpStatusGroup.isVisible = false
            // we should show option to setup 4S
            setupRecoveryButton.isVisible = true
            setupMegolmBackupButton.isVisible = false
            // We let the option to ignore and quit
            exportManuallyButton.isVisible = true
            exitAnywayButton.isVisible = true
            signOutButton.isVisible = false
        } else if (state.keysBackupState == KeysBackupState.Unknown || state.keysBackupState == KeysBackupState.Disabled) {
            sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_no_backup)
            backingUpStatusGroup.isVisible = false
            // no key backup and cannot setup full 4S
            // we propose to setup
            // we should show option to setup 4S
            setupRecoveryButton.isVisible = false
            setupMegolmBackupButton.isVisible = true
            // We let the option to ignore and quit
            exportManuallyButton.isVisible = true
            exitAnywayButton.isVisible = true
            signOutButton.isVisible = false
        } else {
            // so keybackup is setup
            // You should wait until all are uploaded
            setupRecoveryButton.isVisible = false

            when (state.keysBackupState) {
                KeysBackupState.ReadyToBackUp -> {
                    sheetTitle.text = getString(R.string.action_sign_out_confirmation_simple)

                    // Ok all keys are backedUp
                    backingUpStatusGroup.isVisible = true
                    backupProgress.isVisible = false
                    backupCompleteImage.isVisible = true
                    backupStatusTex.text = getString(R.string.keys_backup_info_keys_all_backup_up)

                    setupMegolmBackupButton.isVisible = false
                    exportManuallyButton.isVisible = false
                    exitAnywayButton.isVisible = false
                    // You can signout
                    signOutButton.isVisible = true
                }
                KeysBackupState.WillBackUp,
                KeysBackupState.BackingUp     -> {
                    sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_backing_up)

                    // save in progress
                    backingUpStatusGroup.isVisible = true
                    backupProgress.isVisible = true
                    backupCompleteImage.isVisible = false
                    backupStatusTex.text = getString(R.string.sign_out_bottom_sheet_backing_up_keys)

                    setupMegolmBackupButton.isVisible = false
                    exportManuallyButton.isVisible = false
                    exitAnywayButton.isVisible = true
                    signOutButton.isVisible = false
                }
                KeysBackupState.NotTrusted    -> {
                    sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_backup_not_active)
                    // It's not trusted and we know there are unsaved keys..
                    backingUpStatusGroup.isVisible = false

                    // option to enter pass/key
                    setupMegolmBackupButton.isVisible = true
                    exportManuallyButton.isVisible = true
                    exitAnywayButton.isVisible = true
                    signOutButton.isVisible = false
                }
                else                          -> {
                    // mmm.. strange state

                    exitAnywayButton.isVisible = true
                }
            }
        }

        // final call if keys have been exported
        when (state.hasBeenExportedToFile) {
            is Loading -> {
                signoutExportingLoading.isVisible = true
                backingUpStatusGroup.isVisible = false

                setupRecoveryButton.isVisible = false
                setupMegolmBackupButton.isVisible = false
                exportManuallyButton.isVisible = false
                exitAnywayButton.isVisible = true
                signOutButton.isVisible = false
            }
            is Success -> {
                if (state.hasBeenExportedToFile.invoke()) {
                    sheetTitle.text = getString(R.string.action_sign_out_confirmation_simple)
                    backingUpStatusGroup.isVisible = false

                    setupRecoveryButton.isVisible = false
                    setupMegolmBackupButton.isVisible = false
                    exportManuallyButton.isVisible = false
                    exitAnywayButton.isVisible = false
                    signOutButton.isVisible = true
                }
            }
            else       -> {
            }
        }
        super.invalidate()
    }

    override fun getLayoutResId() = R.layout.bottom_sheet_logout_and_backup

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
