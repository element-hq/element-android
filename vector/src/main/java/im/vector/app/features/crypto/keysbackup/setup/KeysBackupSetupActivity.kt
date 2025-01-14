/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.crypto.keys.KeysExporter
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KeysBackupSetupActivity : SimpleFragmentActivity() {

    override fun getTitleRes() = CommonStrings.title_activity_keys_backup_setup

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    @Inject lateinit var keysExporter: KeysExporter

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
                KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_2 -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(views.container, KeysBackupSetupStep2Fragment::class.java)
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_TO_STEP_3 -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(views.container, KeysBackupSetupStep3Fragment::class.java)
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_FINISH -> {
                    val resultIntent = Intent()
                    viewModel.keysVersion.value?.version?.let {
                        resultIntent.putExtra(KEYS_VERSION, it)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_PROMPT_REPLACE -> {
                    MaterialAlertDialogBuilder(this)
                            .setTitle(CommonStrings.keys_backup_setup_override_backup_prompt_tile)
                            .setMessage(CommonStrings.keys_backup_setup_override_backup_prompt_description)
                            .setPositiveButton(CommonStrings.keys_backup_setup_override_replace) { _, _ ->
                                viewModel.forceCreateKeyBackup(this)
                            }.setNegativeButton(CommonStrings.keys_backup_setup_override_stop) { _, _ ->
                                viewModel.stopAndKeepAfterDetectingExistingOnServer()
                            }
                            .show()
                }
                KeysBackupSetupSharedViewModel.NAVIGATE_MANUAL_EXPORT -> {
                    queryExportKeys(
                            userId = session.myUserId,
                            applicationName = buildMeta.applicationName,
                            activityResultLauncher = saveStartForActivityResult,
                    )
                }
            }
        }

        viewModel.prepareRecoverFailError.observe(this) { error ->
            if (error != null) {
                MaterialAlertDialogBuilder(this)
                        .setTitle(CommonStrings.unknown_error)
                        .setMessage(error.localizedMessage)
                        .setPositiveButton(CommonStrings.ok) { _, _ ->
                            // nop
                            viewModel.prepareRecoverFailError.value = null
                        }
                        .show()
            }
        }

        viewModel.creatingBackupError.observe(this) { error ->
            if (error != null) {
                MaterialAlertDialogBuilder(this)
                        .setTitle(CommonStrings.unexpected_error)
                        .setMessage(error.localizedMessage)
                        .setPositiveButton(CommonStrings.ok) { _, _ ->
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
                toast(getString(CommonStrings.unexpected_error))
                hideWaitingView()
            }
        }
    }

    private fun export(passphrase: String, uri: Uri) {
        lifecycleScope.launch {
            try {
                keysExporter.export(passphrase, uri)
                toast(getString(CommonStrings.encryption_exported_successfully))
                setResult(Activity.RESULT_OK, Intent().apply { putExtra(MANUAL_EXPORT, true) })
                finish()
            } catch (failure: Throwable) {
                toast(failure.localizedMessage ?: getString(CommonStrings.unexpected_error))
            }
            hideWaitingView()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (viewModel.shouldPromptOnBack) {
            if (waitingView?.isVisible == true) {
                return
            }
            MaterialAlertDialogBuilder(this)
                    .setTitle(CommonStrings.keys_backup_setup_skip_title)
                    .setMessage(CommonStrings.keys_backup_setup_skip_msg)
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .setPositiveButton(CommonStrings.action_leave) { _, _ ->
                        finish()
                    }
                    .show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

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
