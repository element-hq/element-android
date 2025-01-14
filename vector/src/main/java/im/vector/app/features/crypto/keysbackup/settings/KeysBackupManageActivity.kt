/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.app.features.crypto.quads.SharedSecureStorageActivity
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME

@AndroidEntryPoint
class KeysBackupManageActivity : SimpleFragmentActivity() {

    companion object {

        fun intent(context: Context): Intent {
            return Intent(context, KeysBackupManageActivity::class.java)
        }
    }

    override fun getTitleRes() = CommonStrings.encryption_message_recovery

    private val viewModel: KeysBackupSettingsViewModel by viewModel()

    private val secretStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = activityResult.data?.getStringExtra(SharedSecureStorageActivity.EXTRA_DATA_RESULT)
            val reset = activityResult.data?.getBooleanExtra(SharedSecureStorageActivity.EXTRA_DATA_RESET, false) ?: false
            if (result != null) {
                viewModel.handle(KeyBackupSettingsAction.StoreIn4SSuccess(result, SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS))
            } else if (reset) {
                // all have been reset so a new backup would have been created
                viewModel.handle(KeyBackupSettingsAction.StoreIn4SReset)
            }
        } else {
            viewModel.handle(KeyBackupSettingsAction.StoreIn4SFailure)
        }
    }

    override fun initUiAndData() {
        super.initUiAndData()
        if (supportFragmentManager.fragments.isEmpty()) {
            replaceFragment(views.container, KeysBackupSettingsFragment::class.java)
            viewModel.handle(KeyBackupSettingsAction.Init)
        }

        // Observe the deletion of keys backup
        viewModel.onEach(KeysBackupSettingViewState::deleteBackupRequest) { asyncDelete ->
            when (asyncDelete) {
                is Fail -> {
                    updateWaitingView(null)

                    MaterialAlertDialogBuilder(this)
                            .setTitle(CommonStrings.unknown_error)
                            .setMessage(getString(CommonStrings.keys_backup_get_version_error, asyncDelete.error.localizedMessage))
                            .setCancelable(false)
                            .setPositiveButton(CommonStrings.ok, null)
                            .show()
                }
                is Loading -> {
                    updateWaitingView(WaitingViewData(getString(CommonStrings.keys_backup_settings_deleting_backup)))
                }
                else -> {
                    updateWaitingView(null)
                }
            }
        }

        viewModel.observeViewEvents {
            when (it) {
                KeysBackupViewEvents.OpenLegacyCreateBackup -> {
                    startActivity(KeysBackupSetupActivity.intent(this, false))
                }
                is KeysBackupViewEvents.RequestStore4SSecret -> {
                    secretStartForActivityResult.launch(
                            SharedSecureStorageActivity.newWriteIntent(
                                    context = this,
                                    writeSecrets = listOf(KEYBACKUP_SECRET_SSSS_NAME to it.recoveryKey)
                            )
                    )
                }
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // When there is no network we could get stuck in infinite loading
        // because backup state will stay in CheckingBackUpOnHomeserver
        if (viewModel.canExit()) {
            finish()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
