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

package im.vector.riotx.features.workers.signout

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import butterknife.BindView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotx.features.crypto.keysbackup.setup.KeysBackupSetupActivity

class SignOutBottomSheetDialogFragment : VectorBaseBottomSheetDialogFragment() {

    @BindView(R.id.bottom_sheet_signout_warning_text)
    lateinit var sheetTitle: TextView

    @BindView(R.id.bottom_sheet_signout_backingup_status_group)
    lateinit var backingUpStatusGroup: ViewGroup

    @BindView(R.id.keys_backup_setup)
    lateinit var setupClickableView: View

    @BindView(R.id.keys_backup_activate)
    lateinit var activateClickableView: View

    @BindView(R.id.keys_backup_dont_want)
    lateinit var dontWantClickableView: View

    @BindView(R.id.bottom_sheet_signout_icon_progress_bar)
    lateinit var backupProgress: ProgressBar

    @BindView(R.id.bottom_sheet_signout_icon)
    lateinit var backupCompleteImage: ImageView

    @BindView(R.id.bottom_sheet_backup_status_text)
    lateinit var backupStatusTex: TextView

    @BindView(R.id.bottom_sheet_signout_button)
    lateinit var signoutClickableView: View

    @BindView(R.id.root_layout)
    lateinit var rootLayout: ViewGroup

    var onSignOut: Runnable? = null

    companion object {
        fun newInstance() = SignOutBottomSheetDialogFragment()

        private const val EXPORT_REQ = 0
    }

    init {
        isCancelable = true
    }

    private lateinit var viewModel: SignOutViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = fragmentViewModelProvider.get(SignOutViewModel::class.java)

        setupClickableView.setOnClickListener {
            context?.let { context ->
                startActivityForResult(KeysBackupSetupActivity.intent(context, true), EXPORT_REQ)
            }
        }

        activateClickableView.setOnClickListener {
            context?.let { context ->
                startActivity(KeysBackupManageActivity.intent(context))
            }
        }

        signoutClickableView.setOnClickListener {
            this.onSignOut?.run()
        }

        dontWantClickableView.setOnClickListener { _ ->
            context?.let {
                AlertDialog.Builder(it)
                        .setTitle(R.string.are_you_sure)
                        .setMessage(R.string.sign_out_bottom_sheet_will_lose_secure_messages)
                        .setPositiveButton(R.string.backup) { _, _ ->
                            when (viewModel.keysBackupState.value) {
                                KeysBackupState.NotTrusted -> {
                                    context?.let { context ->
                                        startActivity(KeysBackupManageActivity.intent(context))
                                    }
                                }
                                KeysBackupState.Disabled   -> {
                                    context?.let { context ->
                                        startActivityForResult(KeysBackupSetupActivity.intent(context, true), EXPORT_REQ)
                                    }
                                }
                                KeysBackupState.BackingUp,
                                KeysBackupState.WillBackUp -> {
                                    // keys are already backing up please wait
                                    context?.toast(R.string.keys_backup_is_not_finished_please_wait)
                                }
                                else                       -> {
                                    // nop
                                }
                            }
                        }
                        .setNegativeButton(R.string.action_sign_out) { _, _ ->
                            onSignOut?.run()
                        }
                        .show()
            }
        }

        viewModel.keysExportedToFile.observe(viewLifecycleOwner, Observer {
            val hasExportedToFile = it ?: false
            if (hasExportedToFile) {
                // We can allow to sign out

                sheetTitle.text = getString(R.string.action_sign_out_confirmation_simple)

                signoutClickableView.isVisible = true
                dontWantClickableView.isVisible = false
                setupClickableView.isVisible = false
                activateClickableView.isVisible = false
                backingUpStatusGroup.isVisible = false
            }
        })

        viewModel.keysBackupState.observe(viewLifecycleOwner, Observer {
            if (viewModel.keysExportedToFile.value == true) {
                // ignore this
                return@Observer
            }
            TransitionManager.beginDelayedTransition(rootLayout)
            when (it) {
                KeysBackupState.ReadyToBackUp -> {
                    signoutClickableView.isVisible = true
                    dontWantClickableView.isVisible = false
                    setupClickableView.isVisible = false
                    activateClickableView.isVisible = false
                    backingUpStatusGroup.isVisible = true

                    backupProgress.isVisible = false
                    backupCompleteImage.isVisible = true
                    backupStatusTex.text = getString(R.string.keys_backup_info_keys_all_backup_up)

                    sheetTitle.text = getString(R.string.action_sign_out_confirmation_simple)
                }
                KeysBackupState.BackingUp,
                KeysBackupState.WillBackUp    -> {
                    backingUpStatusGroup.isVisible = true
                    sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_backing_up)
                    dontWantClickableView.isVisible = true
                    setupClickableView.isVisible = false
                    activateClickableView.isVisible = false

                    backupProgress.isVisible = true
                    backupCompleteImage.isVisible = false
                    backupStatusTex.text = getString(R.string.sign_out_bottom_sheet_backing_up_keys)
                }
                KeysBackupState.NotTrusted    -> {
                    backingUpStatusGroup.isVisible = false
                    dontWantClickableView.isVisible = true
                    setupClickableView.isVisible = false
                    activateClickableView.isVisible = true
                    sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_backup_not_active)
                }
                else                          -> {
                    backingUpStatusGroup.isVisible = false
                    dontWantClickableView.isVisible = true
                    setupClickableView.isVisible = true
                    activateClickableView.isVisible = false
                    sheetTitle.text = getString(R.string.sign_out_bottom_sheet_warning_no_backup)
                }
            }

            // updateSignOutSection()
        })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == EXPORT_REQ) {
                val manualExportDone = data?.getBooleanExtra(KeysBackupSetupActivity.MANUAL_EXPORT, false)
                viewModel.keysExportedToFile.value = manualExportDone
            }
        }
    }
}
