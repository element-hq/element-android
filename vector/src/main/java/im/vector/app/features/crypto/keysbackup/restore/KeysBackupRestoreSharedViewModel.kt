/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.LiveEvent
import im.vector.app.features.session.coroutineScope
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keysbackup.BackupUtils
import org.matrix.android.sdk.api.session.crypto.keysbackup.IBackupRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.session.securestorage.KeyInfoResult
import org.matrix.android.sdk.api.util.fromBase64
import timber.log.Timber
import javax.inject.Inject

class KeysBackupRestoreSharedViewModel @Inject constructor(
        private val stringProvider: StringProvider,
        private val matrix: Matrix,
) : ViewModel() {

    data class KeySource(
            val isInMemory: Boolean,
            val isInQuadS: Boolean
    )

    companion object {
        const val NAVIGATE_TO_RECOVER_WITH_KEY = "NAVIGATE_TO_RECOVER_WITH_KEY"
        const val NAVIGATE_TO_SUCCESS = "NAVIGATE_TO_SUCCESS"
        const val NAVIGATE_TO_4S = "NAVIGATE_TO_4S"
        const val NAVIGATE_FAILED_TO_LOAD_4S = "NAVIGATE_FAILED_TO_LOAD_4S"
    }

    lateinit var session: Session

    var keyVersionResult: MutableLiveData<KeysVersionResult?> = MutableLiveData(null)

    var keySourceModel: MutableLiveData<KeySource> = MutableLiveData()

    private var _keyVersionResultError: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    val keyVersionResultError: LiveData<LiveEvent<String>>
        get() = _keyVersionResultError

    private var _navigateEvent: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    val navigateEvent: LiveData<LiveEvent<String>>
        get() = _navigateEvent

    var loadingEvent: MutableLiveData<WaitingViewData?> = MutableLiveData(null)

    var importKeyResult: ImportRoomKeysResult? = null
    var importRoomKeysFinishWithResult: MutableLiveData<LiveEvent<ImportRoomKeysResult>> = MutableLiveData()

    fun initSession(session: Session) {
        if (!this::session.isInitialized) {
            this.session = session
            viewModelScope.launch {
                getLatestVersion()
            }
        }
    }

    private val progressObserver = object : StepProgressListener {
        override fun onStepProgress(step: StepProgressListener.Step) {
            when (step) {
                is StepProgressListener.Step.ComputingKey -> {
                    loadingEvent.postValue(
                            WaitingViewData(
                                    stringProvider.getString(CommonStrings.keys_backup_restoring_waiting_message) +
                                            "\n" + stringProvider.getString(CommonStrings.keys_backup_restoring_computing_key_waiting_message),
                                    step.progress,
                                    step.total
                            )
                    )
                }
                is StepProgressListener.Step.DownloadingKey -> {
                    loadingEvent.postValue(
                            WaitingViewData(
                                    stringProvider.getString(CommonStrings.keys_backup_restoring_waiting_message) +
                                            "\n" + stringProvider.getString(CommonStrings.keys_backup_restoring_downloading_backup_waiting_message),
                                    isIndeterminate = true
                            )
                    )
                }
                is StepProgressListener.Step.ImportingKey -> {
                    Timber.d("backupKeys.ImportingKey.progress: ${step.progress}")
                    // Progress 0 can take a while, display an indeterminate progress in this case
                    if (step.progress == 0) {
                        loadingEvent.postValue(
                                WaitingViewData(
                                        stringProvider.getString(CommonStrings.keys_backup_restoring_waiting_message) +
                                                "\n" + stringProvider.getString(CommonStrings.keys_backup_restoring_importing_keys_waiting_message),
                                        isIndeterminate = true
                                )
                        )
                    } else {
                        loadingEvent.postValue(
                                WaitingViewData(
                                        stringProvider.getString(CommonStrings.keys_backup_restoring_waiting_message) +
                                                "\n" + stringProvider.getString(CommonStrings.keys_backup_restoring_importing_keys_waiting_message),
                                        step.progress,
                                        step.total
                                )
                        )
                    }
                }
                is StepProgressListener.Step.DecryptingKey -> {
                    if (step.progress == 0) {
                        loadingEvent.postValue(
                                WaitingViewData(
                                        stringProvider.getString(CommonStrings.keys_backup_restoring_waiting_message) +
                                                "\n" + stringProvider.getString(CommonStrings.keys_backup_restoring_importing_keys_waiting_message),
                                        isIndeterminate = true
                                )
                        )
                    } else {
                        loadingEvent.postValue(
                                WaitingViewData(
                                        stringProvider.getString(CommonStrings.keys_backup_restoring_waiting_message) +
                                                "\n" + stringProvider.getString(CommonStrings.keys_backup_restoring_importing_keys_waiting_message),
                                        step.progress,
                                        step.total
                                )
                        )
                    }
                }
            }
        }
    }

    private suspend fun getLatestVersion() {
        val keysBackup = session.cryptoService().keysBackupService()

        loadingEvent.postValue(WaitingViewData(stringProvider.getString(CommonStrings.keys_backup_restore_is_getting_backup_version)))

        try {
            val version = keysBackup.getCurrentVersion()?.toKeysVersionResult()
            if (version?.version == null) {
                loadingEvent.postValue(null)
                _keyVersionResultError.postValue(LiveEvent(stringProvider.getString(CommonStrings.keys_backup_get_version_error, "")))
                return
            }

            keyVersionResult.postValue(version)
            // Let's check if there is quads
            val isBackupKeyInQuadS = isBackupKeyInQuadS()

            val savedSecret = session.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()
            if (savedSecret != null && savedSecret.version == version.version) {
                // key is in memory!
                keySourceModel.postValue(
                        KeySource(isInMemory = true, isInQuadS = true)
                )
                // Go and use it!!
                try {
                    recoverUsingBackupRecoveryKey(savedSecret.recoveryKey, version)
                } catch (failure: Throwable) {
                    Timber.e(failure, "## recoverUsingBackupRecoveryKey FAILED")
                    keySourceModel.postValue(
                            KeySource(isInMemory = false, isInQuadS = true)
                    )
                }
            } else if (isBackupKeyInQuadS) {
                // key is in QuadS!
                keySourceModel.postValue(
                        KeySource(isInMemory = false, isInQuadS = true)
                )
                _navigateEvent.postValue(LiveEvent(NAVIGATE_TO_4S))
            } else {
                // we need to restore directly
                keySourceModel.postValue(
                        KeySource(isInMemory = false, isInQuadS = false)
                )
            }

            loadingEvent.postValue(null)
        } catch (failure: Throwable) {
            loadingEvent.postValue(null)
            _keyVersionResultError.postValue(LiveEvent(stringProvider.getString(CommonStrings.keys_backup_get_version_error, failure.localizedMessage)))
        }
    }

    fun handleGotSecretFromSSSS(cipherData: String, alias: String) {
        try {
            cipherData.fromBase64().inputStream().use { ins ->
                val res = matrix.secureStorageService().loadSecureSecret<Map<String, String>>(ins, alias)
                val secret = res?.get(KEYBACKUP_SECRET_SSSS_NAME)
                if (secret == null) {
                    _navigateEvent.postValue(
                            LiveEvent(NAVIGATE_FAILED_TO_LOAD_4S)
                    )
                    return
                }
                loadingEvent.postValue(WaitingViewData(stringProvider.getString(CommonStrings.keys_backup_restore_is_getting_backup_version)))

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val computedRecoveryKey = computeRecoveryKey(secret.fromBase64())
                        val backupRecoveryKey = BackupUtils.recoveryKeyFromBase58(computedRecoveryKey)
                        recoverUsingBackupRecoveryKey(backupRecoveryKey)
                    } catch (failure: Throwable) {
                        _navigateEvent.postValue(
                                LiveEvent(NAVIGATE_FAILED_TO_LOAD_4S)
                        )
                    }
                }
            }
        } catch (failure: Throwable) {
            _navigateEvent.postValue(
                    LiveEvent(NAVIGATE_FAILED_TO_LOAD_4S)
            )
        }
    }

    suspend fun recoverUsingBackupPass(passphrase: String) {
        val keysBackup = session.cryptoService().keysBackupService()
        val keyVersion = keyVersionResult.value ?: return

        loadingEvent.postValue(WaitingViewData(stringProvider.getString(CommonStrings.loading)))

        try {
            val result = keysBackup.restoreKeyBackupWithPassword(
                    keyVersion,
                    passphrase,
                    null,
                    session.myUserId,
                    progressObserver
            )
            loadingEvent.postValue(null)
            didRecoverSucceed(result)
            trustOnDecrypt(keysBackup, keyVersion)
        } catch (failure: Throwable) {
            loadingEvent.postValue(null)
            throw failure
        }
    }

    suspend fun recoverUsingBackupRecoveryKey(recoveryKey: IBackupRecoveryKey, keyVersion: KeysVersionResult? = null) {
        val keysBackup = session.cryptoService().keysBackupService()
        // This is badddddd
        val version = keyVersion ?: keyVersionResult.value ?: return

        loadingEvent.postValue(WaitingViewData(stringProvider.getString(CommonStrings.loading)))

        try {
            val result = keysBackup.restoreKeysWithRecoveryKey(
                    version,
                    recoveryKey,
                    null,
                    session.myUserId,
                    progressObserver
            )
            loadingEvent.postValue(null)
            withContext(Dispatchers.Main) {
                didRecoverSucceed(result)
                trustOnDecrypt(keysBackup, version)
            }
        } catch (failure: Throwable) {
            Timber.e(failure, "##  restoreKeysWithRecoveryKey failure")
            loadingEvent.postValue(null)
            throw failure
        }
    }

    private fun isBackupKeyInQuadS(): Boolean {
        val sssBackupSecret = session.accountDataService().getUserAccountDataEvent(KEYBACKUP_SECRET_SSSS_NAME)
                ?: return false

        // Some sanity ?
        val defaultKeyResult = session.sharedSecretStorageService().getDefaultKey()
        val keyInfo = (defaultKeyResult as? KeyInfoResult.Success)?.keyInfo
                ?: return false

        return (sssBackupSecret.content["encrypted"] as? Map<*, *>)?.containsKey(keyInfo.id) == true
    }

    private fun trustOnDecrypt(keysBackup: KeysBackupService, keysVersionResult: KeysVersionResult) {
        // do that on session scope because could happen outside of view model lifecycle
        session.coroutineScope.launch {
            tryOrNull("## Failed to trustKeysBackupVersion") {
                keysBackup.trustKeysBackupVersion(keysVersionResult, true)
            }
        }
    }

    fun moveToRecoverWithKey() {
        _navigateEvent.postValue(LiveEvent(NAVIGATE_TO_RECOVER_WITH_KEY))
    }

    private fun didRecoverSucceed(result: ImportRoomKeysResult) {
        importKeyResult = result
        _navigateEvent.postValue(LiveEvent(NAVIGATE_TO_SUCCESS))
    }
}
