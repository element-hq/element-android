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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import im.vector.app.R
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.crypto.keys.KeysExporter
import org.matrix.android.sdk.api.MatrixCallback

class KeysBackupSetupActivity : SimpleFragmentActivity() {

    override fun getTitleRes() = R.string.title_activity_keys_backup_setup

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    override fun initUiAndData() {
        super.initUiAndData()
        if (isFirstCreation()) {
            replaceFragment(R.id.container, KeysBackupSetupStep1Fragment::class.java)
        }

        viewModel = viewModelProvider.get(KeysBackupSetupSharedViewModel::class.java)
        viewModel.showManualExport.value = intent.getBooleanExtra(EXTRA_SHOW_MANUAL_EXPORT, false)
        viewModel.initSession(session)

        viewModel.isCreatingBackupVersion.observe(this, Observer {
            val isCreating = it ?: false
            if (isCreating) {
                showWaitingView()
            } else {
                hideWaitingView()
            }
        })

        viewModel.loadingStatus.observe(this, Observer {
            it?.let {
                updateWaitingView(it)
            }
        })

        viewModel.navigateEvent.observeEvent(this) { uxStateEvent ->
            when (uxStateEvent) {
                KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_2      -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(R.id.container, KeysBackupSetupStep2Fragment::class.java)
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_3      -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(R.id.container, KeysBackupSetupStep3Fragment::class.java)
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
                    AlertDialog.Builder(this)
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
                    queryExportKeys(session.myUserId, REQUEST_CODE_SAVE_MEGOLM_EXPORT)
                }
            }
        }

        viewModel.prepareRecoverFailError.observe(this, Observer { error ->
            if (error != null) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.unknown_error)
                        .setMessage(error.localizedMessage)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            // nop
                            viewModel.prepareRecoverFailError.value = null
                        }
                        .show()
            }
        })

        viewModel.creatingBackupError.observe(this, Observer { error ->
            if (error != null) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.unexpected_error)
                        .setMessage(error.localizedMessage)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            // nop
                            viewModel.creatingBackupError.value = null
                        }
                        .show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SAVE_MEGOLM_EXPORT) {
            val uri = data?.data
            if (resultCode == Activity.RESULT_OK && uri != null) {
                ExportKeysDialog().show(this, object : ExportKeysDialog.ExportKeyDialogListener {
                    override fun onPassphrase(passphrase: String) {
                        showWaitingView()

                        KeysExporter(session)
                                .export(this@KeysBackupSetupActivity,
                                        passphrase,
                                        uri,
                                        object : MatrixCallback<Boolean> {
                                            override fun onSuccess(data: Boolean) {
                                                if (data) {
                                                    toast(getString(R.string.encryption_exported_successfully))
                                                    Intent().apply {
                                                        putExtra(MANUAL_EXPORT, true)
                                                    }.let {
                                                        setResult(Activity.RESULT_OK, it)
                                                        finish()
                                                    }
                                                }
                                                hideWaitingView()
                                            }

                                            override fun onFailure(failure: Throwable) {
                                                toast(failure.localizedMessage ?: getString(R.string.unexpected_error))
                                                hideWaitingView()
                                            }
                                        })
                    }
                })
            } else {
                toast(getString(R.string.unexpected_error))
                hideWaitingView()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (viewModel.shouldPromptOnBack) {
            if (waitingView?.isVisible == true) {
                return
            }
            AlertDialog.Builder(this)
                    .setTitle(R.string.keys_backup_setup_skip_title)
                    .setMessage(R.string.keys_backup_setup_skip_msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.leave) { _, _ ->
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
        const val REQUEST_CODE_SAVE_MEGOLM_EXPORT = 101

        fun intent(context: Context, showManualExport: Boolean): Intent {
            val intent = Intent(context, KeysBackupSetupActivity::class.java)
            intent.putExtra(EXTRA_SHOW_MANUAL_EXPORT, showManualExport)
            return intent
        }
    }
}
