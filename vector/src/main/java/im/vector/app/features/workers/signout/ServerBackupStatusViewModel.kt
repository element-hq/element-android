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

package im.vector.app.features.workers.signout

import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.flow.flow

data class ServerBackupStatusViewState(
        val bannerState: Async<BannerState> = Uninitialized
) : MavericksState

/**
 * The state representing the view
 * It can take one state at a time
 */
sealed class BannerState {

    object Hidden : BannerState()

    // Keys backup is not setup, numberOfKeys is the number of locally stored keys
    data class Setup(val numberOfKeys: Int) : BannerState()

    // Keys are backing up
    object BackingUp : BannerState()
}

class ServerBackupStatusViewModel @AssistedInject constructor(@Assisted initialState: ServerBackupStatusViewState,
                                                              private val session: Session) :
        VectorViewModel<ServerBackupStatusViewState, EmptyAction, EmptyViewEvents>(initialState), KeysBackupStateListener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ServerBackupStatusViewModel, ServerBackupStatusViewState> {
        override fun create(initialState: ServerBackupStatusViewState): ServerBackupStatusViewModel
    }

    companion object : MavericksViewModelFactory<ServerBackupStatusViewModel, ServerBackupStatusViewState> by hiltMavericksViewModelFactory()

    // Keys exported manually
    val keysExportedToFile = MutableLiveData<Boolean>()
    val keysBackupState = MutableLiveData<KeysBackupState>()

    private val keyBackupFlow = MutableSharedFlow<KeysBackupState>(0)

    init {
        session.cryptoService().keysBackupService().addListener(this)
        keysBackupState.value = session.cryptoService().keysBackupService().state
        val liveUserAccountData = session.flow().liveUserAccountData(setOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME))
        val liveCrossSigningInfo = session.flow().liveCrossSigningInfo(session.myUserId)
        val liveCrossSigningPrivateKeys = session.flow().liveCrossSigningPrivateKeys()
        combine(liveUserAccountData, liveCrossSigningInfo, keyBackupFlow, liveCrossSigningPrivateKeys) { _, crossSigningInfo, keyBackupState, pInfo ->
            // first check if 4S is already setup
            if (session.sharedSecretStorageService.isRecoverySetup()) {
                // 4S is already setup sp we should not display anything
                return@combine when (keyBackupState) {
                    KeysBackupState.BackingUp -> BannerState.BackingUp
                    else                      -> BannerState.Hidden
                }
            }

            // So recovery is not setup
            // Check if cross signing is enabled and local secrets known
            if (
                    crossSigningInfo.getOrNull() == null ||
                    (crossSigningInfo.getOrNull()?.isTrusted() == true &&
                            pInfo.getOrNull()?.allKnown().orFalse())
            ) {
                // So 4S is not setup and we have local secrets,
                return@combine BannerState.Setup(numberOfKeys = getNumberOfKeysToBackup())
            }
            BannerState.Hidden
        }
                .sample(1000) // we don't want to flicker or catch transient states
                .distinctUntilChanged()
                .execute { async ->
                    copy(
                            bannerState = async
                    )
                }

        viewModelScope.launch {
            keyBackupFlow.tryEmit(session.cryptoService().keysBackupService().state)
        }
    }

    /**
     * Safe way to get the current KeysBackup version
     */
    fun getCurrentBackupVersion(): String {
        return session.cryptoService().keysBackupService().currentBackupVersion ?: ""
    }

    /**
     * Safe way to get the number of keys to backup
     */
    fun getNumberOfKeysToBackup(): Int {
        return session.cryptoService().inboundGroupSessionsCount(false)
    }

    /**
     * Safe way to tell if there are more keys on the server
     */
    fun canRestoreKeys(): Boolean {
        return session.cryptoService().keysBackupService().canRestoreKeys()
    }

    override fun onCleared() {
        session.cryptoService().keysBackupService().removeListener(this)
        super.onCleared()
    }

    override fun onStateChange(newState: KeysBackupState) {
        viewModelScope.launch {
            keyBackupFlow.tryEmit(session.cryptoService().keysBackupService().state)
        }
        keysBackupState.value = newState
    }

    fun refreshRemoteStateIfNeeded() {
        if (keysBackupState.value == KeysBackupState.Disabled) {
            session.cryptoService().keysBackupService().checkAndStartKeysBackup()
        }
    }

    override fun handle(action: EmptyAction) {}
}
