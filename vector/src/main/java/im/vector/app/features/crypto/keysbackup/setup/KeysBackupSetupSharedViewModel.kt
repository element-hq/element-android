/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.keysbackup.setup

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nulabinc.zxcvbn.Strength
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.utils.LiveEvent
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.IBackupRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersion
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import timber.log.Timber
import javax.inject.Inject

/**
 * The shared view model between all fragments.
 */
class KeysBackupSetupSharedViewModel @Inject constructor(
        private val clock: Clock,
) : ViewModel() {

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
    var recoveryKey: MutableLiveData<IBackupRecoveryKey?> = MutableLiveData(null)
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
        currentRequestId.value = clock.epochMillis()
        isCreatingBackupVersion.value = true

        recoveryKey.value = null
        prepareRecoverFailError.value = null
        val requestedId = currentRequestId.value!!
        val progressListener = object : ProgressListener {
            override fun onProgress(progress: Int, total: Int) {
                if (requestedId != currentRequestId.value) {
                    // this is an old request, we can't cancel but we can ignore
                    return
                }

                loadingStatus.postValue(
                        WaitingViewData(
                                context.getString(CommonStrings.keys_backup_setup_step3_generating_key_status),
                                progress,
                                total
                        )
                )
            }
        }

        viewModelScope.launch {
            try {
                val data = session.cryptoService().keysBackupService().prepareKeysBackupVersion(withPassphrase, progressListener)
                if (requestedId != currentRequestId.value) {
                    // this is an old request, we can't cancel but we can ignore
                    return@launch
                }
                recoveryKey.postValue(data.recoveryKey)
                megolmBackupCreationInfo = data
                copyHasBeenMade = false

                val keyBackup = session.cryptoService().keysBackupService()
                createKeysBackup(context, keyBackup)
            } catch (failure: Throwable) {
                if (requestedId != currentRequestId.value) {
                    // this is an old request, we can't cancel but we can ignore
                    return@launch
                }

                loadingStatus.postValue(null)
                isCreatingBackupVersion.postValue(false)
                prepareRecoverFailError.postValue(failure)
            }
        }
    }

    fun forceCreateKeyBackup(context: Context) {
        val keyBackup = session.cryptoService().keysBackupService()
        createKeysBackup(context, keyBackup, true)
    }

    fun stopAndKeepAfterDetectingExistingOnServer() {
        loadingStatus.postValue(null)
        navigateEvent.postValue(LiveEvent(NAVIGATE_FINISH))
        viewModelScope.launch {
            session.cryptoService().keysBackupService().checkAndStartKeysBackup()
        }
    }

    private fun createKeysBackup(context: Context, keysBackup: KeysBackupService, forceOverride: Boolean = false) {
        loadingStatus.value = WaitingViewData(context.getString(CommonStrings.keys_backup_setup_creating_backup), isIndeterminate = true)

        creatingBackupError.value = null

        viewModelScope.launch {
            try {
                val data = keysBackup.getCurrentVersion()?.toKeysVersionResult()
                if (data?.version.isNullOrBlank() || forceOverride) {
                    processOnCreate(keysBackup)
                } else {
                    loadingStatus.postValue(null)
                    // we should prompt
                    isCreatingBackupVersion.postValue(false)
                    navigateEvent.postValue(LiveEvent(NAVIGATE_PROMPT_REPLACE))
                }
            } catch (failure: Throwable) {
                Timber.w(failure, "Failed to createKeysBackup")
            }
        }
    }

    suspend fun processOnCreate(keysBackup: KeysBackupService) {
        try {
            loadingStatus.postValue(null)
            val created = keysBackup.createKeysBackupVersion(megolmBackupCreationInfo!!)
            isCreatingBackupVersion.postValue(false)
            keysVersion.postValue(created)
            navigateEvent.value = LiveEvent(NAVIGATE_TO_STEP_3)
        } catch (failure: Throwable) {
            Timber.e(failure, "## createKeyBackupVersion")
            loadingStatus.postValue(null)

            isCreatingBackupVersion.postValue(false)
            creatingBackupError.postValue(failure)
        }
    }
}
