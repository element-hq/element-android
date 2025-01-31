/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.restore

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.vector.app.R
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class KeysBackupRestoreFromKeyViewModel @Inject constructor(
        private val stringProvider: StringProvider
) : ViewModel() {

    var recoveryCode: MutableLiveData<String?> = MutableLiveData(null)
    var recoveryCodeErrorText: MutableLiveData<String?> = MutableLiveData(null)

    // ========= Actions =========
    fun updateCode(newValue: String) {
        recoveryCode.value = newValue
        recoveryCodeErrorText.value = null
    }

    fun recoverKeys(sharedViewModel: KeysBackupRestoreSharedViewModel) {
        sharedViewModel.loadingEvent.postValue(WaitingViewData(stringProvider.getString(R.string.loading)))
        recoveryCodeErrorText.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val recoveryKey = recoveryCode.value!!
            try {
                sharedViewModel.recoverUsingBackupRecoveryKey(recoveryKey)
            } catch (failure: Throwable) {
                recoveryCodeErrorText.postValue(stringProvider.getString(R.string.keys_backup_recovery_code_error_decrypt))
            }
        }
    }
}
