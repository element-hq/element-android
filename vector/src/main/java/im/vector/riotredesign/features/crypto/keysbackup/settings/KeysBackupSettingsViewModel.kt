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

import com.airbnb.mvrx.*
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupStateListener
import im.vector.matrix.android.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get


class KeysBackupSettingsViewModel(initialState: KeysBackupSettingViewState,
                                  session: Session) : VectorViewModel<KeysBackupSettingViewState>(initialState),
        KeysBackupStateListener {

    companion object : MvRxViewModelFactory<KeysBackupSettingsViewModel, KeysBackupSettingViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: KeysBackupSettingViewState): KeysBackupSettingsViewModel? {
            val session = viewModelContext.activity.get<Session>()

            val firstState = state.copy(
                    keysBackupState = session.getKeysBackupService().state,
                    keysBackupVersion = session.getKeysBackupService().keysBackupVersion
            )

            return KeysBackupSettingsViewModel(firstState, session)
        }
    }

    private var keysBackupService: KeysBackupService = session.getKeysBackupService()

    init {
        keysBackupService.addListener(this)

        getKeysBackupTrust()
    }

    fun init() {
        keysBackupService.forceUsingLastVersion(object : MatrixCallback<Boolean> {})
    }

    fun getKeysBackupTrust() = withState { state ->
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
        super.onCleared()
        keysBackupService.removeListener(this)
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

    fun deleteCurrentBackup() {
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
        val currentBackupState = keysBackupService.state

        return currentBackupState == KeysBackupState.Unknown
                || currentBackupState == KeysBackupState.CheckingBackUpOnHomeserver
    }
}
