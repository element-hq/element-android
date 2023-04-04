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
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrust
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersion
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.api.util.toBase64NoPadding
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
            KeyBackupSettingsAction.DeleteKeyBackup -> deleteCurrentBackup()
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
        keysBackupService.forceUsingLastVersion(NoOpMatrixCallback())
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

            keysBackupService
                    .getKeysBackupTrust(versionResult, object : MatrixCallback<KeysBackupVersionTrust> {
                        override fun onSuccess(data: KeysBackupVersionTrust) {
                            setState {
                                copy(
                                        keysBackupVersionTrust = Success(data)
                                )
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            setState {
                                copy(
                                        keysBackupVersionTrust = Fail(failure)
                                )
                            }
                        }
                    })
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

        getKeysBackupTrust()
    }

    suspend fun setUpKeyBackup() {
        // We need to check if 4S is enabled first.
        // If it is we need to use it, generate a random key
        // for the backup and store it in the 4S
        if (session.sharedSecretStorageService().isRecoverySetup()) {
            val creationInfo = awaitCallback<MegolmBackupCreationInfo> {
                session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, null, it)
            }
            pendingBackupCreationInfo = creationInfo
            val recoveryKey = extractCurveKeyFromRecoveryKey(creationInfo.recoveryKey)?.toBase64NoPadding()
            _viewEvents.post(KeysBackupViewEvents.RequestStore4SSecret(recoveryKey!!))
        } else {
            // No 4S so we can open legacy flow
            _viewEvents.post(KeysBackupViewEvents.OpenLegacyCreateBackup)
        }
    }

    suspend fun completeBackupCreation() {
        val info = pendingBackupCreationInfo ?: return
        try {
            val version = awaitCallback<KeysVersion> {
                session.cryptoService().keysBackupService().createKeysBackupVersion(info, it)
            }
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

    private fun deleteCurrentBackup() {
        val keysBackupService = keysBackupService

        if (keysBackupService.currentBackupVersion != null) {
            setState {
                copy(
                        deleteBackupRequest = Loading()
                )
            }

            keysBackupService.deleteBackup(keysBackupService.currentBackupVersion!!, object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    setState {
                        copy(
                                keysBackupVersion = null,
                                keysBackupVersionTrust = Uninitialized,
                                // We do not care about the success data
                                deleteBackupRequest = Uninitialized
                        )
                    }
                }

                override fun onFailure(failure: Throwable) {
                    setState {
                        copy(
                                deleteBackupRequest = Fail(failure)
                        )
                    }
                }
            })
        }
    }

    fun canExit(): Boolean {
        val currentBackupState = keysBackupService.getState()

        return currentBackupState == KeysBackupState.Unknown ||
                currentBackupState == KeysBackupState.CheckingBackUpOnHomeserver
    }
}
