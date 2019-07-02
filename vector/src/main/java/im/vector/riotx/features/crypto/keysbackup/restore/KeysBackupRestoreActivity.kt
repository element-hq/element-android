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
package im.vector.riotx.features.crypto.keysbackup.restore

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import im.vector.fragments.keysbackup.restore.KeysBackupRestoreFromPassphraseFragment
import im.vector.riotx.R
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.SimpleFragmentActivity

class KeysBackupRestoreActivity : SimpleFragmentActivity() {

    companion object {

        fun intent(context: Context): Intent {
            return Intent(context, KeysBackupRestoreActivity::class.java)
        }
    }

    override fun getTitleRes() = R.string.title_activity_keys_backup_restore

    private lateinit var viewModel: KeysBackupRestoreSharedViewModel

    override fun initUiAndData() {
        super.initUiAndData()
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(KeysBackupRestoreSharedViewModel::class.java)
        viewModel.initSession(session)
        viewModel.keyVersionResult.observe(this, Observer { keyVersion ->

            if (keyVersion != null && supportFragmentManager.fragments.isEmpty()) {
                val isBackupCreatedFromPassphrase = keyVersion.getAuthDataAsMegolmBackupAuthData().privateKeySalt != null
                if (isBackupCreatedFromPassphrase) {
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.container, KeysBackupRestoreFromPassphraseFragment.newInstance())
                            .commitNow()
                } else {
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.container, KeysBackupRestoreFromKeyFragment.newInstance())
                            .commitNow()
                }
            }
        })

        viewModel.keyVersionResultError.observeEvent(this) { message ->
            AlertDialog.Builder(this)
                    .setTitle(R.string.unknown_error)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        //nop
                        finish()
                    }
                    .show()
        }

        if (viewModel.keyVersionResult.value == null) {
            //We need to fetch from API
            viewModel.getLatestVersion(this)
        }

        viewModel.navigateEvent.observeEvent(this) { uxStateEvent ->
            when (uxStateEvent) {
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_RECOVER_WITH_KEY -> {
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.container, KeysBackupRestoreFromKeyFragment.newInstance())
                            .addToBackStack(null)
                            .commit()
                }
                KeysBackupRestoreSharedViewModel.NAVIGATE_TO_SUCCESS          -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.container, KeysBackupRestoreSuccessFragment.newInstance())
                            .commit()
                }
            }
        }

        viewModel.loadingEvent.observe(this, Observer {
            updateWaitingView(it)
        })

        viewModel.importRoomKeysFinishWithResult.observeEvent(this) {
            //set data?
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}