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

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.ui.views.KeysBackupBanner
import im.vector.app.features.crypto.quads.SharedSecureStorageActivity
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import javax.inject.Inject

@AndroidEntryPoint
class KeysBackupRestoreActivity : SimpleFragmentActivity() {

    companion object {
        const val SECRET_ALIAS = SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS

        fun intent(context: Context): Intent {
            return Intent(context, KeysBackupRestoreActivity::class.java)
        }
    }

    override fun getTitleRes() = R.string.title_activity_keys_backup_restore

    private lateinit var viewModel: KeysBackupRestoreSharedViewModel

    override fun onBackPressed() {
        hideWaitingView()
        super.onBackPressed()
    }

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder

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
                    .setTitle(R.string.unknown_error)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        // nop
                        finish()
                    }
                    .show()
        }

        if (viewModel.keyVersionResult.value == null) {
            // We need to fetch from API
            viewModel.getLatestVersion()
        }

        viewModel.navigateEvent.observeEvent(this) { uxStateEvent ->
            when (uxStateEvent) {
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_RECOVER_WITH_KEY -> {
                    addFragmentToBackstack(views.container, KeysBackupRestoreFromKeyFragment::class.java, allowStateLoss = true)
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_SUCCESS          -> {
                    viewModel.keyVersionResult.value?.version?.let {
                        KeysBackupBanner.onRecoverDoneForVersion(this, it)
                    }
                    replaceFragment(views.container, KeysBackupRestoreSuccessFragment::class.java, allowStateLoss = true)
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_4S               -> {
                    launch4SActivity()
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_FAILED_TO_LOAD_4S   -> {
                    MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.unknown_error)
                            .setMessage(R.string.error_failed_to_import_keys)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok) { _, _ ->
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
        SharedSecureStorageActivity.newIntent(
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
