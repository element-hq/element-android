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

package im.vector.riotx.features.workers.signout

import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupStateListener
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.store.PrivateKeysInfo
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountData
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import io.reactivex.Observable
import io.reactivex.functions.Function4
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

data class ServerBackupStatusViewState(
        val bannerState: Async<BannerState> = Uninitialized
) : MvRxState

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
                                                              private val session: Session)
    : VectorViewModel<ServerBackupStatusViewState, EmptyAction, EmptyViewEvents>(initialState), KeysBackupStateListener {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: ServerBackupStatusViewState): ServerBackupStatusViewModel
    }

    companion object : MvRxViewModelFactory<ServerBackupStatusViewModel, ServerBackupStatusViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ServerBackupStatusViewState): ServerBackupStatusViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    // Keys exported manually
    val keysExportedToFile = MutableLiveData<Boolean>()
    val keysBackupState = MutableLiveData<KeysBackupState>()

    private val keyBackupPublishSubject: PublishSubject<KeysBackupState> = PublishSubject.create()

    init {
        session.cryptoService().keysBackupService().addListener(this)

        keyBackupPublishSubject.onNext(session.cryptoService().keysBackupService().state)
        keysBackupState.value = session.cryptoService().keysBackupService().state
        session.rx().liveCrossSigningPrivateKeys()
        Observable.combineLatest<List<UserAccountData>, Optional<MXCrossSigningInfo>, KeysBackupState, Optional<PrivateKeysInfo>, BannerState>(
                session.rx().liveAccountData(setOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME)),
                session.rx().liveCrossSigningInfo(session.myUserId),
                keyBackupPublishSubject,
                session.rx().liveCrossSigningPrivateKeys(),
                Function4 { _, crossSigningInfo, keyBackupState, pInfo ->
                    // first check if 4S is already setup
                    if (session.sharedSecretStorageService.isRecoverySetup()) {
                        // 4S is already setup sp we should not display anything
                        return@Function4 when (keyBackupState) {
                            KeysBackupState.WillBackUp,
                            KeysBackupState.BackingUp -> BannerState.BackingUp
                            else                      -> BannerState.Hidden
                        }
                    }

                    // So recovery is not setup
                    // Check if cross signing is enabled and local secrets known
                    if (crossSigningInfo.getOrNull()?.isTrusted() == true
                            && pInfo.getOrNull()?.master != null
                            && pInfo.getOrNull()?.selfSigned != null
                            && pInfo.getOrNull()?.user != null
                    ) {
                        // So 4S is not setup and we have local secrets,
                        return@Function4 BannerState.Setup(numberOfKeys = getNumberOfKeysToBackup())
                    }

                    BannerState.Hidden
                }
        )
                .throttleLast(800, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .execute { async ->
                    copy(
                            bannerState = async
                    )
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
        super.onCleared()
        session.cryptoService().keysBackupService().removeListener(this)
    }

    override fun onStateChange(newState: KeysBackupState) {
        keyBackupPublishSubject.onNext(session.cryptoService().keysBackupService().state)
        keysBackupState.value = newState
    }

    fun refreshRemoteStateIfNeeded() {
        if (keysBackupState.value == KeysBackupState.Disabled) {
            session.cryptoService().keysBackupService().checkAndStartKeysBackup()
        }
    }

    override fun handle(action: EmptyAction) {}
}
