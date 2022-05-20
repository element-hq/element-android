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
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrust

class KeysBackupSettingsViewModel @AssistedInject constructor(@Assisted initialState: KeysBackupSettingViewState,
                                                              session: Session
) : VectorViewModel<KeysBackupSettingViewState, KeyBackupSettingsAction, EmptyViewEvents>(initialState),
        KeysBackupStateListener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<KeysBackupSettingsViewModel, KeysBackupSettingViewState> {
        override fun create(initialState: KeysBackupSettingViewState): KeysBackupSettingsViewModel
    }

    companion object : MavericksViewModelFactory<KeysBackupSettingsViewModel, KeysBackupSettingViewState> by hiltMavericksViewModelFactory()

    private val cryptoService = session.cryptoService()
    private val keysBackupService: KeysBackupService = session.cryptoService().keysBackupService()

    init {
        setState {
            this.copy(
                    keysBackupState = keysBackupService.state,
                    keysBackupVersion = keysBackupService.keysBackupVersion
            )
        }
        keysBackupService.addListener(this)
        getKeysBackupTrust()
    }

    override fun handle(action: KeyBackupSettingsAction) {
        when (action) {
            KeyBackupSettingsAction.Init              -> init()
            KeyBackupSettingsAction.GetKeyBackupTrust -> getKeysBackupTrust()
            KeyBackupSettingsAction.DeleteKeyBackup   -> deleteCurrentBackup()
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
            setState { copy(deleteBackupRequest = Uninitialized) }
            suspend {
                keysBackupService.getKeysBackupTrust(versionResult)
            }.execute {
                copy(keysBackupVersionTrust = it)
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

    private fun deleteCurrentBackup() {
        val keysBackupService = keysBackupService

        val currentBackupVersion = keysBackupService.currentBackupVersion
        if (currentBackupVersion != null) {
            setState {
                copy(
                        deleteBackupRequest = Loading()
                )
            }
            viewModelScope.launch {
                try {
                    keysBackupService.deleteBackup(currentBackupVersion)
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
    }

    fun canExit(): Boolean {
        val currentBackupState = keysBackupService.state

        return currentBackupState == KeysBackupState.Unknown ||
                currentBackupState == KeysBackupState.CheckingBackUpOnHomeserver
    }
}
