/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.workers.signout

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
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
 * The state representing the view.
 * It can take one state at a time.
 */
sealed interface BannerState {
    // Not yet rendered
    object Initial : BannerState

    // View will be Gone
    object Hidden : BannerState

    // Keys backup is not setup, numberOfKeys is the number of locally stored keys
    data class Setup(val numberOfKeys: Int, val doNotShowAgain: Boolean) : BannerState

    // Keys backup can be recovered, with version from the server
    data class Recover(val version: String, val doNotShowForVersion: String) : BannerState

    // Keys backup can be updated
    data class Update(val version: String, val doNotShowForVersion: String) : BannerState

    // Keys are backing up
    object BackingUp : BannerState
}

class ServerBackupStatusViewModel @AssistedInject constructor(
        @Assisted initialState: ServerBackupStatusViewState,
        private val session: Session,
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
) :
        VectorViewModel<ServerBackupStatusViewState, ServerBackupStatusAction, EmptyViewEvents>(initialState), KeysBackupStateListener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ServerBackupStatusViewModel, ServerBackupStatusViewState> {
        override fun create(initialState: ServerBackupStatusViewState): ServerBackupStatusViewModel
    }

    companion object : MavericksViewModelFactory<ServerBackupStatusViewModel, ServerBackupStatusViewState> by hiltMavericksViewModelFactory() {
        /**
         * Preference key for setup. Value is a boolean.
         */
        private const val BANNER_SETUP_DO_NOT_SHOW_AGAIN = "BANNER_SETUP_DO_NOT_SHOW_AGAIN"

        /**
         * Preference key for recover. Value is a backup version (String).
         */
        private const val BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION = "BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION"

        /**
         * Preference key for update. Value is a backup version (String).
         */
        private const val BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION = "BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION"
    }

    // Keys exported manually
    val keysExportedToFile = MutableLiveData<Boolean>()
    val keysBackupState = MutableLiveData<KeysBackupState>()

    private val keyBackupFlow = MutableSharedFlow<KeysBackupState>(0)

    init {
        session.cryptoService().keysBackupService().addListener(this)
        keysBackupState.value = session.cryptoService().keysBackupService().getState()
        val liveUserAccountData = session.flow().liveUserAccountData(setOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME))
        val liveCrossSigningInfo = session.flow().liveCrossSigningInfo(session.myUserId)
        val liveCrossSigningPrivateKeys = session.flow().liveCrossSigningPrivateKeys()
        combine(liveUserAccountData, liveCrossSigningInfo, keyBackupFlow, liveCrossSigningPrivateKeys) { _, crossSigningInfo, keyBackupState, pInfo ->
            // first check if 4S is already setup
            if (session.sharedSecretStorageService().isRecoverySetup()) {
                // 4S is already setup sp we should not display anything
                return@combine when (keyBackupState) {
                    KeysBackupState.BackingUp -> BannerState.BackingUp
                    else -> BannerState.Hidden
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
                return@combine BannerState.Setup(
                        numberOfKeys = getNumberOfKeysToBackup(),
                        doNotShowAgain = sharedPreferences.getBoolean(BANNER_SETUP_DO_NOT_SHOW_AGAIN, false)
                )
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
            keyBackupFlow.tryEmit(session.cryptoService().keysBackupService().getState())
        }
    }

    /**
     * Safe way to get the current KeysBackup version.
     */
    fun getCurrentBackupVersion(): String {
        return session.cryptoService().keysBackupService().currentBackupVersion ?: ""
    }

    /**
     * Safe way to get the number of keys to backup.
     */
    private suspend fun getNumberOfKeysToBackup(): Int {
        return session.cryptoService().inboundGroupSessionsCount(false)
    }

    /**
     * Safe way to tell if there are more keys on the server.
     */
    private suspend fun canRestoreKeys(): Boolean {
        return session.cryptoService().keysBackupService().canRestoreKeys()
    }

    override fun onCleared() {
        session.cryptoService().keysBackupService().removeListener(this)
        super.onCleared()
    }

    override fun onStateChange(newState: KeysBackupState) {
        viewModelScope.launch {
            keyBackupFlow.tryEmit(session.cryptoService().keysBackupService().getState())
        }
        keysBackupState.value = newState
    }

    fun refreshRemoteStateIfNeeded() {
        if (keysBackupState.value == KeysBackupState.Disabled) {
            viewModelScope.launch {
                session.cryptoService().keysBackupService().checkAndStartKeysBackup()
            }
        }
    }

    override fun handle(action: ServerBackupStatusAction) {
        when (action) {
            is ServerBackupStatusAction.OnRecoverDoneForVersion -> handleOnRecoverDoneForVersion(action)
            ServerBackupStatusAction.OnBannerDisplayed -> handleOnBannerDisplayed()
            ServerBackupStatusAction.OnBannerClosed -> handleOnBannerClosed()
        }
    }

    private fun handleOnRecoverDoneForVersion(action: ServerBackupStatusAction.OnRecoverDoneForVersion) {
        sharedPreferences.edit {
            putString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, action.version)
        }
    }

    private fun handleOnBannerDisplayed() {
        sharedPreferences.edit {
            putBoolean(BANNER_SETUP_DO_NOT_SHOW_AGAIN, false)
            putString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, "")
        }
    }

    private fun handleOnBannerClosed() = withState { state ->
        when (val bannerState = state.bannerState()) {
            is BannerState.Setup -> {
                sharedPreferences.edit {
                    putBoolean(BANNER_SETUP_DO_NOT_SHOW_AGAIN, true)
                }
            }
            is BannerState.Recover -> {
                sharedPreferences.edit {
                    putString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, bannerState.version)
                }
            }
            is BannerState.Update -> {
                sharedPreferences.edit {
                    putString(BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION, bannerState.version)
                }
            }
            else -> {
                // Should not happen, close button is not displayed in other cases
            }
        }
    }
}
