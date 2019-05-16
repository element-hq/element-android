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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import im.vector.fragments.keysbackup.settings.KeysBackupSettingsFragment
import im.vector.matrix.android.api.MatrixCallback
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.SimpleFragmentActivity


class KeysBackupManageActivity : SimpleFragmentActivity() {

    companion object {

        fun intent(context: Context): Intent {
            val intent = Intent(context, KeysBackupManageActivity::class.java)
            return intent
        }
    }

    override fun getTitleRes() = R.string.encryption_message_recovery


    private lateinit var viewModel: KeysBackupSettingsViewModel

    override fun initUiAndData() {
        super.initUiAndData()
        viewModel = ViewModelProviders.of(this).get(KeysBackupSettingsViewModel::class.java)
        viewModel.initSession(mSession)


        if (supportFragmentManager.fragments.isEmpty()) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, KeysBackupSettingsFragment.newInstance())
                    .commitNow()

            mSession.getKeysBackupService()
                    .forceUsingLastVersion(object : MatrixCallback<Boolean> {})
        }

        viewModel.loadingEvent.observe(this, Observer {
            updateWaitingView(it)
        })


        viewModel.apiResultError.observe(this, Observer { uxStateEvent ->
            uxStateEvent?.getContentIfNotHandled()?.let {
                AlertDialog.Builder(this)
                        .setTitle(R.string.unknown_error)
                        .setMessage(it)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
        })

    }
}