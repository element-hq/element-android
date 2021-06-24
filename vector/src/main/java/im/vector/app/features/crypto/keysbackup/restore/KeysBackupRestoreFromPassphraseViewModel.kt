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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class KeysBackupRestoreFromPassphraseViewModel @Inject constructor(
        private val stringProvider: StringProvider
) : ViewModel() {

    var passphrase: MutableLiveData<String> = MutableLiveData()
    var passphraseErrorText: MutableLiveData<String> = MutableLiveData()

    init {
        passphrase.value = null
        passphraseErrorText.value = null
    }

    // ========= Actions =========

    fun updatePassphrase(newValue: String) {
        passphrase.value = newValue
        passphraseErrorText.value = null
    }

    fun recoverKeys(sharedViewModel: KeysBackupRestoreSharedViewModel) {
        passphraseErrorText.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sharedViewModel.recoverUsingBackupPass(passphrase.value!!)
            } catch (failure: Throwable) {
                passphraseErrorText.postValue(stringProvider.getString(R.string.keys_backup_passphrase_error_decrypt))
            }
        }
    }
}
