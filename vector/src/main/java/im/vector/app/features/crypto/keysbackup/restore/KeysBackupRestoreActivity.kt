/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.restore

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.features.crypto.quads.SharedSecureStorageActivity
import im.vector.app.features.workers.signout.ServerBackupStatusAction
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME

@AndroidEntryPoint
class KeysBackupRestoreActivity : SimpleFragmentActivity() {

    companion object {
        const val SECRET_ALIAS = SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS

        fun intent(context: Context): Intent {
            return Intent(context, KeysBackupRestoreActivity::class.java)
        }
    }

    override fun getTitleRes() = CommonStrings.title_activity_keys_backup_restore

    private lateinit var viewModel: KeysBackupRestoreSharedViewModel
    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by viewModel()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        hideWaitingView()
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun initUiAndData() {
        super.initUiAndData()
        viewModel = viewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)
        viewModel.initSession(activeSessionHolder.getActiveSession())

        viewModel.keySourceModel.observe(this) { keySource ->
            if (keySource != null && !keySource.isInQuadS && supportFragmentManager.fragments.isEmpty()) {
                val isBackupCreatedFromPassphrase =
                        viewModel.keyVersionResult.value?.getAuthDataAsMegolmBackupAuthData()?.privateKeySalt != null
                if (isBackupCreatedFromPassphrase) {
                    replaceFragment(views.container, KeysBackupRestoreFromPassphraseFragment::class.java)
                } else {
                    replaceFragment(views.container, KeysBackupRestoreFromKeyFragment::class.java)
                }
            }
        }

        viewModel.keyVersionResultError.observeEvent(this) { message ->
            MaterialAlertDialogBuilder(this)
                    .setTitle(CommonStrings.unknown_error)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(CommonStrings.ok) { _, _ ->
                        // nop
                        finish()
                    }
                    .show()
        }

        viewModel.navigateEvent.observeEvent(this) { uxStateEvent ->
            when (uxStateEvent) {
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_RECOVER_WITH_KEY -> {
                    addFragmentToBackstack(views.container, KeysBackupRestoreFromKeyFragment::class.java, allowStateLoss = true)
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_SUCCESS -> {
                    viewModel.keyVersionResult.value?.version?.let {
                        // Inform the banner that a Recover has been done for this version, so do not show the Recover banner for this version.
                        serverBackupStatusViewModel.handle(ServerBackupStatusAction.OnRecoverDoneForVersion(it))
                    }
                    replaceFragment(views.container, KeysBackupRestoreSuccessFragment::class.java, allowStateLoss = true)
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_4S -> {
                    launch4SActivity()
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_FAILED_TO_LOAD_4S -> {
                    MaterialAlertDialogBuilder(this)
                            .setTitle(CommonStrings.unknown_error)
                            .setMessage(CommonStrings.error_failed_to_import_keys)
                            .setCancelable(false)
                            .setPositiveButton(CommonStrings.ok) { _, _ ->
                                // nop
                                launch4SActivity()
                            }
                            .show()
                }
            }
        }

        viewModel.loadingEvent.observe(this) {
            updateWaitingView(it)
        }

        viewModel.importRoomKeysFinishWithResult.observeEvent(this) {
            // set data?
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun launch4SActivity() {
        SharedSecureStorageActivity.newReadIntent(
                context = this,
                keyId = null, // default key
                requestedSecrets = listOf(KEYBACKUP_SECRET_SSSS_NAME),
                resultKeyStoreAlias = SECRET_ALIAS
        ).let {
            secretStartForActivityResult.launch(it)
        }
    }

    private val secretStartForActivityResult = registerStartForActivityResult { activityResult ->
        val extraResult = activityResult.data?.getStringExtra(SharedSecureStorageActivity.EXTRA_DATA_RESULT)
        if (activityResult.resultCode == Activity.RESULT_OK && extraResult != null) {
            viewModel.handleGotSecretFromSSSS(
                    extraResult,
                    SECRET_ALIAS
            )
        } else {
            finish()
        }
    }
}
