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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.crypto.keys.KeysExporter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KeysBackupSetupActivity : SimpleFragmentActivity() {

    override fun getTitleRes() = R.string.title_activity_keys_backup_setup

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    @Inject lateinit var keysExporter: KeysExporter
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder

    private val session by lazy {
        activeSessionHolder.getActiveSession()
    }

    override fun initUiAndData() {
        super.initUiAndData()
        if (isFirstCreation()) {
            replaceFragment(views.container, KeysBackupSetupStep1Fragment::class.java)
        }

        viewModel = viewModelProvider.get(KeysBackupSetupSharedViewModel::class.java)
        viewModel.showManualExport.value = intent.getBooleanExtra(EXTRA_SHOW_MANUAL_EXPORT, false)
        viewModel.initSession(session)

        viewModel.isCreatingBackupVersion.observe(this) {
            val isCreating = it ?: false
            if (isCreating) {
                showWaitingView()
            } else {
                hideWaitingView()
            }
        }

        viewModel.loadingStatus.observe(this) {
            it?.let {
                updateWaitingView(it)
            }
        }

        viewModel.navigateEvent.observeEvent(this) { uxStateEvent ->
            when (uxStateEvent) {
                KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_2      -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(views.container, KeysBackupSetupStep2Fragment::class.java)
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_3      -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(views.container, KeysBackupSetupStep3Fragment::class.java)
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_FINISH         -> {
                    val resultIntent = Intent()
                    viewModel.keysVersion.value?.version?.let {
                        resultIntent.putExtra(KEYS_VERSION, it)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_PROMPT_REPLACE -> {
                    MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.keys_backup_setup_override_backup_prompt_tile)
                            .setMessage(R.string.keys_backup_setup_override_backup_prompt_description)
                            .setPositiveButton(R.string.keys_backup_setup_override_replace) { _, _ ->
                                viewModel.forceCreateKeyBackup(this)
                            }.setNegativeButton(R.string.keys_backup_setup_override_stop) { _, _ ->
                                viewModel.stopAndKeepAfterDetectingExistingOnServer()
                            }
                            .show()
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_MANUAL_EXPORT  -> {
                    queryExportKeys(session.myUserId, saveStartForActivityResult)
                }
            }
        }

        viewModel.prepareRecoverFailError.observe(this) { error ->
            if (error != null) {
                MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.unknown_error)
                        .setMessage(error.localizedMessage)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            // nop
                            viewModel.prepareRecoverFailError.value = null
                        }
                        .show()
            }
        }

        viewModel.creatingBackupError.observe(this) { error ->
            if (error != null) {
                MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.unexpected_error)
                        .setMessage(error.localizedMessage)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            // nop
                            viewModel.creatingBackupError.value = null
                        }
                        .show()
            }
        }
    }

    private val saveStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val uri = activityResult.data?.data
            if (uri != null) {
                ExportKeysDialog().show(this, object : ExportKeysDialog.ExportKeyDialogListener {
                    override fun onPassphrase(passphrase: String) {
                        showWaitingView()
                        export(passphrase, uri)
                    }
                })
            } else {
                toast(getString(R.string.unexpected_error))
                hideWaitingView()
            }
        }
    }

    private fun export(passphrase: String, uri: Uri) {
        lifecycleScope.launch {
            try {
                keysExporter.export(passphrase, uri)
                toast(getString(R.string.encryption_exported_successfully))
                setResult(Activity.RESULT_OK, Intent().apply { putExtra(MANUAL_EXPORT, true) })
                finish()
            } catch (failure: Throwable) {
                toast(failure.localizedMessage ?: getString(R.string.unexpected_error))
            }
            hideWaitingView()
        }
    }

    override fun onBackPressed() {
        if (viewModel.shouldPromptOnBack) {
            if (waitingView?.isVisible == true) {
                return
            }
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.keys_backup_setup_skip_title)
                    .setMessage(R.string.keys_backup_setup_skip_msg)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_leave) { _, _ ->
                        finish()
                    }
                    .show()
        } else {
            super.onBackPressed()
        }
    }

//    I think this code is useful, but it violates the code quality rules
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == android .R. id.  home) {
//            onBackPressed()
//            return true
//        }
//
//        return super.onOptionsItemSelected(item)
//    }

    companion object {
        const val KEYS_VERSION = "KEYS_VERSION"
        const val MANUAL_EXPORT = "MANUAL_EXPORT"
        const val EXTRA_SHOW_MANUAL_EXPORT = "SHOW_MANUAL_EXPORT"

        fun intent(context: Context, showManualExport: Boolean): Intent {
            val intent = Intent(context, KeysBackupSetupActivity::class.java)
            intent.putExtra(EXTRA_SHOW_MANUAL_EXPORT, showManualExport)
            return intent
        }
    }
}
