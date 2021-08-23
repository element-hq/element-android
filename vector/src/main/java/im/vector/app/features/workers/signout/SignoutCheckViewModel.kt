/*
 * Copyright (c) 2020 New Vector Ltd
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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.crypto.keys.KeysExporter
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.rx.rx
import timber.log.Timber

data class SignoutCheckViewState(
        val userId: String = "",
        val backupIsSetup: Boolean = false,
        val crossSigningSetupAllKeysKnown: Boolean = false,
        val keysBackupState: KeysBackupState = KeysBackupState.Unknown,
        val hasBeenExportedToFile: Async<Boolean> = Uninitialized
) : MvRxState

class SignoutCheckViewModel @AssistedInject constructor(
        @Assisted initialState: SignoutCheckViewState,
        private val session: Session,
        private val keysExporter: KeysExporter
) : VectorViewModel<SignoutCheckViewState, SignoutCheckViewModel.Actions, EmptyViewEvents>(initialState), KeysBackupStateListener {

    sealed class Actions : VectorViewModelAction {
        data class ExportKeys(val passphrase: String, val uri: Uri) : Actions()
        object KeySuccessfullyManuallyExported : Actions()
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: SignoutCheckViewState): SignoutCheckViewModel
    }

    companion object : MvRxViewModelFactory<SignoutCheckViewModel, SignoutCheckViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SignoutCheckViewState): SignoutCheckViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        session.cryptoService().keysBackupService().addListener(this)
        session.cryptoService().keysBackupService().checkAndStartKeysBackup()

        val quad4SIsSetup = session.sharedSecretStorageService.isRecoverySetup()
        val allKeysKnown = session.cryptoService().crossSigningService().allPrivateKeysKnown()
        val backupState = session.cryptoService().keysBackupService().state
        setState {
            copy(
                    userId = session.myUserId,
                    crossSigningSetupAllKeysKnown = allKeysKnown,
                    backupIsSetup = quad4SIsSetup,
                    keysBackupState = backupState
            )
        }

        session.rx().liveUserAccountData(setOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME))
                .map {
                    session.sharedSecretStorageService.isRecoverySetup()
                }
                .distinctUntilChanged()
                .execute {
                    copy(backupIsSetup = it.invoke() == true)
                }
    }

    override fun onCleared() {
        session.cryptoService().keysBackupService().removeListener(this)
        super.onCleared()
    }

    override fun onStateChange(newState: KeysBackupState) {
        setState {
            copy(
                    keysBackupState = newState
            )
        }
    }

    fun refreshRemoteStateIfNeeded() = withState { state ->
        if (state.keysBackupState == KeysBackupState.Disabled) {
            session.cryptoService().keysBackupService().checkAndStartKeysBackup()
        }
    }

    override fun handle(action: Actions) {
        when (action) {
            is Actions.ExportKeys                   -> handleExportKeys(action)
            Actions.KeySuccessfullyManuallyExported -> {
                setState {
                    copy(hasBeenExportedToFile = Success(true))
                }
            }
        }.exhaustive
    }

    private fun handleExportKeys(action: Actions.ExportKeys) {
        setState {
            copy(hasBeenExportedToFile = Loading())
        }

        viewModelScope.launch {
            val newState = try {
                keysExporter.export(action.passphrase, action.uri)
                Success(true)
            } catch (failure: Throwable) {
                Timber.e("## Failed to export manually keys ${failure.localizedMessage}")
                Uninitialized
            }

            setState {
                copy(hasBeenExportedToFile = newState)
            }
        }
    }
}
