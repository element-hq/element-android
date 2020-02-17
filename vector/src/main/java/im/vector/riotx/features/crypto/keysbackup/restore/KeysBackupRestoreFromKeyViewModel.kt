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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.listeners.StepProgressListener
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.internal.crypto.keysbackup.model.rest.KeysVersionResult
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.riotx.R
import im.vector.riotx.core.platform.WaitingViewData
import im.vector.riotx.core.ui.views.KeysBackupBanner
import timber.log.Timber
import javax.inject.Inject

class KeysBackupRestoreFromKeyViewModel @Inject constructor() : ViewModel() {

    var recoveryCode: MutableLiveData<String> = MutableLiveData()
    var recoveryCodeErrorText: MutableLiveData<String> = MutableLiveData()

    init {
        recoveryCode.value = null
        recoveryCodeErrorText.value = null
    }

    // ========= Actions =========
    fun updateCode(newValue: String) {
        recoveryCode.value = newValue
        recoveryCodeErrorText.value = null
    }

    fun recoverKeys(context: Context, sharedViewModel: KeysBackupRestoreSharedViewModel) {
        val session = sharedViewModel.session
        val keysBackup = session.cryptoService().keysBackupService()

        recoveryCodeErrorText.value = null
        val recoveryKey = recoveryCode.value!!

        val keysVersionResult = sharedViewModel.keyVersionResult.value!!

        keysBackup.restoreKeysWithRecoveryKey(keysVersionResult,
                recoveryKey,
                null,
                session.myUserId,
                object : StepProgressListener {
                    override fun onStepProgress(step: StepProgressListener.Step) {
                        when (step) {
                            is StepProgressListener.Step.DownloadingKey -> {
                                sharedViewModel.loadingEvent.value = WaitingViewData(context.getString(R.string.keys_backup_restoring_waiting_message)
                                        + "\n" + context.getString(R.string.keys_backup_restoring_downloading_backup_waiting_message),
                                        isIndeterminate = true)
                            }
                            is StepProgressListener.Step.ImportingKey -> {
                                // Progress 0 can take a while, display an indeterminate progress in this case
                                if (step.progress == 0) {
                                    sharedViewModel.loadingEvent.value = WaitingViewData(context.getString(R.string.keys_backup_restoring_waiting_message)
                                            + "\n" + context.getString(R.string.keys_backup_restoring_importing_keys_waiting_message),
                                            isIndeterminate = true)
                                } else {
                                    sharedViewModel.loadingEvent.value = WaitingViewData(context.getString(R.string.keys_backup_restoring_waiting_message)
                                            + "\n" + context.getString(R.string.keys_backup_restoring_importing_keys_waiting_message),
                                            step.progress,
                                            step.total)
                                }
                            }
                        }
                    }
                },
                object : MatrixCallback<ImportRoomKeysResult> {
                    override fun onSuccess(data: ImportRoomKeysResult) {
                        sharedViewModel.loadingEvent.value = null
                        sharedViewModel.didRecoverSucceed(data)

                        KeysBackupBanner.onRecoverDoneForVersion(context, keysVersionResult.version!!)
                        trustOnDecrypt(keysBackup, keysVersionResult)
                    }

                    override fun onFailure(failure: Throwable) {
                        sharedViewModel.loadingEvent.value = null
                        recoveryCodeErrorText.value = context.getString(R.string.keys_backup_recovery_code_error_decrypt)
                        Timber.e(failure, "## onUnexpectedError")
                    }
                })
    }

    private fun trustOnDecrypt(keysBackup: KeysBackupService, keysVersionResult: KeysVersionResult) {
        keysBackup.trustKeysBackupVersion(keysVersionResult, true,
                object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        Timber.v("##### trustKeysBackupVersion onSuccess")
                    }
                })
    }
}
