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

package im.vector.app.features.crypto.keysbackup.setup

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nulabinc.zxcvbn.Strength
import im.vector.app.R
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.utils.LiveEvent
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupLastVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.toKeysVersionResult
import timber.log.Timber
import javax.inject.Inject

/**
 * The shared view model between all fragments.
 */
class KeysBackupSetupSharedViewModel @Inject constructor() : ViewModel() {

    companion object {
        const val NAVIGATE_TO_STEP_2 = "NAVIGATE_TO_STEP_2"
        const val NAVIGATE_TO_STEP_3 = "NAVIGATE_TO_STEP_3"
        const val NAVIGATE_PROMPT_REPLACE = "NAVIGATE_PROMPT_REPLACE"
        const val NAVIGATE_FINISH = "NAVIGATE_FINISH"
        const val NAVIGATE_MANUAL_EXPORT = "NAVIGATE_MANUAL_EXPORT"
    }

    lateinit var session: Session

    val userId: String
        get() = session.myUserId

    var showManualExport: MutableLiveData<Boolean> = MutableLiveData()

    var navigateEvent: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    var shouldPromptOnBack = true

    // Step 2
    var passphrase: MutableLiveData<String> = MutableLiveData()
    var passphraseError: MutableLiveData<String> = MutableLiveData()

    var confirmPassphrase: MutableLiveData<String> = MutableLiveData()
    var confirmPassphraseError: MutableLiveData<String> = MutableLiveData()

    var passwordStrength: MutableLiveData<Strength> = MutableLiveData()

    // Step 3
    // Var to ignore events from previous request(s) to generate a recovery key
    private var currentRequestId: MutableLiveData<Long> = MutableLiveData()
    var recoveryKey: MutableLiveData<String?> = MutableLiveData(null)
    var prepareRecoverFailError: MutableLiveData<Throwable?> = MutableLiveData(null)
    var megolmBackupCreationInfo: MegolmBackupCreationInfo? = null
    var copyHasBeenMade = false
    var isCreatingBackupVersion: MutableLiveData<Boolean> = MutableLiveData(false)
    var creatingBackupError: MutableLiveData<Throwable?> = MutableLiveData(null)
    var keysVersion: MutableLiveData<KeysVersion> = MutableLiveData()

    var loadingStatus: MutableLiveData<WaitingViewData?> = MutableLiveData(null)

    fun initSession(session: Session) {
        this.session = session
    }

    fun prepareRecoveryKey(context: Context, withPassphrase: String?) {
        // Update requestId
        currentRequestId.value = System.currentTimeMillis()
        isCreatingBackupVersion.value = true

        recoveryKey.value = null
        prepareRecoverFailError.value = null
        session.let { mxSession ->
            val requestedId = currentRequestId.value!!

            mxSession.cryptoService().keysBackupService().prepareKeysBackupVersion(withPassphrase,
                    object : ProgressListener {
                        override fun onProgress(progress: Int, total: Int) {
                            if (requestedId != currentRequestId.value) {
                                // this is an old request, we can't cancel but we can ignore
                                return
                            }

                            loadingStatus.value = WaitingViewData(context.getString(R.string.keys_backup_setup_step3_generating_key_status),
                                    progress,
                                    total)
                        }
                    },
                    object : MatrixCallback<MegolmBackupCreationInfo> {
                        override fun onSuccess(data: MegolmBackupCreationInfo) {
                            if (requestedId != currentRequestId.value) {
                                // this is an old request, we can't cancel but we can ignore
                                return
                            }
                            recoveryKey.value = data.recoveryKey
                            megolmBackupCreationInfo = data
                            copyHasBeenMade = false

                            val keyBackup = session.cryptoService().keysBackupService()
                            createKeysBackup(context, keyBackup)
                        }

                        override fun onFailure(failure: Throwable) {
                            if (requestedId != currentRequestId.value) {
                                // this is an old request, we can't cancel but we can ignore
                                return
                            }

                            loadingStatus.value = null

                            isCreatingBackupVersion.value = false
                            prepareRecoverFailError.value = failure
                        }
                    })
        }
    }

    fun forceCreateKeyBackup(context: Context) {
        val keyBackup = session.cryptoService().keysBackupService()
        createKeysBackup(context, keyBackup, true)
    }

    fun stopAndKeepAfterDetectingExistingOnServer() {
        loadingStatus.value = null
        navigateEvent.value = LiveEvent(NAVIGATE_FINISH)
        session.cryptoService().keysBackupService().checkAndStartKeysBackup()
    }

    private fun createKeysBackup(context: Context, keysBackup: KeysBackupService, forceOverride: Boolean = false) {
        loadingStatus.value = WaitingViewData(context.getString(R.string.keys_backup_setup_creating_backup), isIndeterminate = true)

        creatingBackupError.value = null

        keysBackup.getCurrentVersion(object : MatrixCallback<KeysBackupLastVersionResult> {
            override fun onSuccess(data: KeysBackupLastVersionResult) {
                if (data.toKeysVersionResult()?.version.isNullOrBlank() || forceOverride) {
                    processOnCreate()
                } else {
                    loadingStatus.value = null
                    // we should prompt
                    isCreatingBackupVersion.value = false
                    navigateEvent.value = LiveEvent(NAVIGATE_PROMPT_REPLACE)
                }
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## createKeyBackupVersion")
                loadingStatus.value = null

                isCreatingBackupVersion.value = false
                creatingBackupError.value = failure
            }

            fun processOnCreate() {
                keysBackup.createKeysBackupVersion(megolmBackupCreationInfo!!, object : MatrixCallback<KeysVersion> {
                    override fun onSuccess(data: KeysVersion) {
                        loadingStatus.value = null

                        isCreatingBackupVersion.value = false
                        keysVersion.value = data
                        navigateEvent.value = LiveEvent(NAVIGATE_TO_STEP_3)
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## createKeyBackupVersion")
                        loadingStatus.value = null

                        isCreatingBackupVersion.value = false
                        creatingBackupError.value = failure
                    }
                })
            }
        })
    }
}
