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
package im.vector.riotredesign.features.crypto.keysbackup.settings

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.viewModel
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.SimpleFragmentActivity
import im.vector.riotredesign.core.platform.WaitingViewData
import im.vector.riotredesign.features.crypto.keysbackup.KeysBackupModule
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope


class KeysBackupManageActivity : SimpleFragmentActivity() {

    companion object {

        fun intent(context: Context): Intent {
            return Intent(context, KeysBackupManageActivity::class.java)
        }
    }

    override fun getTitleRes() = R.string.encryption_message_recovery

    private val viewModel: KeysBackupSettingsViewModel by viewModel()

    override fun initUiAndData() {
        super.initUiAndData()

        bindScope(getOrCreateScope(KeysBackupModule.KEYS_BACKUP_SCOPE))

        if (supportFragmentManager.fragments.isEmpty()) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, KeysBackupSettingsFragment.newInstance())
                    .commitNow()

            viewModel.init()
        }

        // Observe the deletion of keys backup
        viewModel.selectSubscribe(this, KeysBackupSettingViewState::deleteBackupRequest) { asyncDelete ->
            when (asyncDelete) {
                is Fail    -> {
                    updateWaitingView(null)

                    AlertDialog.Builder(this)
                            .setTitle(R.string.unknown_error)
                            .setMessage(getString(R.string.keys_backup_get_version_error, asyncDelete.error.localizedMessage))
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
                is Loading -> {
                    updateWaitingView(WaitingViewData(getString(R.string.keys_backup_settings_deleting_backup)))
                }
                else       -> {
                    updateWaitingView(null)
                }
            }
        }
    }
}