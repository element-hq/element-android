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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import im.vector.matrix.android.internal.crypto.keysbackup.model.rest.KeysVersionResult
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.WaitingViewData
import im.vector.riotredesign.core.utils.LiveEvent


class KeysBackupSettingsViewModel : ViewModel(),
        KeysBackupService.KeysBackupStateListener {

    var session: Session? = null

    var keyVersionTrust: MutableLiveData<KeysBackupVersionTrust> = MutableLiveData()
    var keyBackupState: MutableLiveData<KeysBackupState> = MutableLiveData()

    private var _apiResultError: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    val apiResultError: LiveData<LiveEvent<String>>
        get() = _apiResultError

    var loadingEvent: MutableLiveData<WaitingViewData> = MutableLiveData()

    fun initSession(session: Session) {
        keyBackupState.value = session.getKeysBackupService().state
        if (this.session == null) {
            this.session = session
            session.getKeysBackupService().addListener(this)
        }
    }

    fun getKeysBackupTrust(versionResult: KeysVersionResult) {
        val keysBackup = session?.getKeysBackupService()
        keysBackup?.getKeysBackupTrust(versionResult, object : MatrixCallback<KeysBackupVersionTrust> {
            override fun onSuccess(data: KeysBackupVersionTrust) {
                keyVersionTrust.value = data
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        session?.getKeysBackupService()?.removeListener(this)
    }

    override fun onStateChange(newState: KeysBackupState) {
        keyBackupState.value = newState
    }

    fun deleteCurrentBackup(context: Context) {
        session?.getKeysBackupService()?.run {
            loadingEvent.value = WaitingViewData(context.getString(R.string.keys_backup_settings_deleting_backup))
            if (currentBackupVersion != null) {
                deleteBackup(currentBackupVersion!!, object : MatrixCallback<Unit> {
                    override fun onSuccess(info: Unit) {
                        //mmmm if state is stil unknown/checking..
                        loadingEvent.value = null
                    }

                    override fun onFailure(failure: Throwable) {
                        loadingEvent.value = null
                        _apiResultError.value = LiveEvent(context.getString(R.string.keys_backup_get_version_error, failure.localizedMessage))
                    }
                })
            }
        }
    }
}