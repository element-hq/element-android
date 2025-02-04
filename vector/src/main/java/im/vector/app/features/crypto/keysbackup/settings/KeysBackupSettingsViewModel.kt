/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.settings

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import timber.log.Timber

class KeysBackupSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: KeysBackupSettingViewState,
        private val session: Session
) : VectorViewModel<KeysBackupSettingViewState, KeyBackupSettingsAction, KeysBackupViewEvents>(initialState),
        KeysBackupStateListener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<KeysBackupSettingsViewModel, KeysBackupSettingViewState> {
        override fun create(initialState: KeysBackupSettingViewState): KeysBackupSettingsViewModel
    }

    companion object : MavericksViewModelFactory<KeysBackupSettingsViewModel, KeysBackupSettingViewState> by hiltMavericksViewModelFactory()

    private val cryptoService = session.cryptoService()
    private val keysBackupService: KeysBackupService = session.cryptoService().keysBackupService()

    var pendingBackupCreationInfo: MegolmBackupCreationInfo? = null

    init {
        setState {
            this.copy(
                    keysBackupState = keysBackupService.getState(),
                    keysBackupVersion = keysBackupService.keysBackupVersion
            )
        }
        keysBackupService.addListener(this)
        getKeysBackupTrust()
    }

    override fun handle(action: KeyBackupSettingsAction) {
        when (action) {
            KeyBackupSettingsAction.Init -> init()
            KeyBackupSettingsAction.GetKeyBackupTrust -> getKeysBackupTrust()
            KeyBackupSettingsAction.DeleteKeyBackup -> viewModelScope.launch {
                deleteCurrentBackup()
            }
            KeyBackupSettingsAction.SetUpKeyBackup -> viewModelScope.launch {
                setUpKeyBackup()
            }
            KeyBackupSettingsAction.StoreIn4SReset,
            KeyBackupSettingsAction.StoreIn4SFailure -> {
                pendingBackupCreationInfo = null
                // nothing to do just stay on fragment
            }
            is KeyBackupSettingsAction.StoreIn4SSuccess -> viewModelScope.launch { completeBackupCreation() }
        }
    }

    private fun init() {
        viewModelScope.launch {
            keysBackupService.forceUsingLastVersion()
        }
    }

    private fun getKeysBackupTrust() = withState { state ->
        val versionResult = keysBackupService.keysBackupVersion

        if (state.keysBackupVersionTrust is Uninitialized && versionResult != null) {
            setState {
                copy(
                        keysBackupVersionTrust = Loading(),
                        deleteBackupRequest = Uninitialized
                )
            }
            viewModelScope.launch {
                val trust = keysBackupService.getKeysBackupTrust(versionResult)
                setState {
                    copy(
                            keysBackupVersionTrust = Success(trust)
                    )
                }
            }
        }
    }

    override fun onCleared() {
        keysBackupService.removeListener(this)
        super.onCleared()
    }

    override fun onStateChange(newState: KeysBackupState) {
        setState {
            copy(
                    keysBackupState = newState,
                    keysBackupVersion = keysBackupService.keysBackupVersion
            )
        }
        when (newState) {
            KeysBackupState.BackingUp, KeysBackupState.WillBackUp -> updateKeysCount()
            else                                                  -> Unit
        }
        getKeysBackupTrust()
    }

    private fun updateKeysCount() {
        viewModelScope.launch {
            val totalKeys = cryptoService.inboundGroupSessionsCount(false)
            val backedUpKeys = cryptoService.inboundGroupSessionsCount(true)
            val remainingKeysToBackup = totalKeys - backedUpKeys
            setState {
                copy(remainingKeysToBackup = remainingKeysToBackup)
            }
        }
    }

    suspend fun setUpKeyBackup() {
        // We need to check if 4S is enabled first.
        // If it is we need to use it, generate a random key
        // for the backup and store it in the 4S
        if (session.sharedSecretStorageService().isRecoverySetup()) {
            val creationInfo = session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null)
            pendingBackupCreationInfo = creationInfo
            val recoveryKey = creationInfo.recoveryKey.toBase64()
            _viewEvents.post(KeysBackupViewEvents.RequestStore4SSecret(recoveryKey))
        } else {
            // No 4S so we can open legacy flow
            _viewEvents.post(KeysBackupViewEvents.OpenLegacyCreateBackup)
        }
    }

    suspend fun completeBackupCreation() {
        val info = pendingBackupCreationInfo ?: return
        try {
            val version = session.cryptoService().keysBackupService().createKeysBackupVersion(info)
            // Save it for gossiping
            Timber.d("## BootstrapCrossSigningTask: Creating 4S - Save megolm backup key for gossiping")
            session.cryptoService().keysBackupService().saveBackupRecoveryKey(info.recoveryKey, version = version.version)
        } catch (failure: Throwable) {
            // XXX mm... failed we should remove what we put in 4S, as it was not created?
            // for now just stay on the screen, user can retry, there is no api to delete account data
        } finally {
            pendingBackupCreationInfo = null
        }
    }

    private suspend fun deleteCurrentBackup() {
        val keysBackupService = keysBackupService

        if (keysBackupService.currentBackupVersion != null) {
            setState {
                copy(
                        deleteBackupRequest = Loading()
                )
            }

            try {
                keysBackupService.deleteBackup(keysBackupService.currentBackupVersion!!)
                setState {
                    copy(
                            keysBackupVersion = null,
                            keysBackupVersionTrust = Uninitialized,
                            // We do not care about the success data
                            deleteBackupRequest = Uninitialized
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            deleteBackupRequest = Fail(failure)
                    )
                }
            }
        }
    }

    fun canExit(): Boolean {
        val currentBackupState = keysBackupService.getState()

        return currentBackupState == KeysBackupState.Unknown ||
                currentBackupState == KeysBackupState.CheckingBackUpOnHomeserver
    }
}
