/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.quads

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.securestorage.IntegrityResult
import org.matrix.android.sdk.api.session.securestorage.KeyInfo
import org.matrix.android.sdk.api.session.securestorage.KeyInfoResult
import org.matrix.android.sdk.api.session.securestorage.KeyRef
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import java.io.ByteArrayOutputStream

sealed class RequestType {
    data class ReadSecrets(val secretsName: List<String>) : RequestType()
    data class WriteSecrets(val secretsNameValue: List<Pair<String, String>>) : RequestType()
}

data class SharedSecureStorageViewState(
        val ready: Boolean = false,
        val hasPassphrase: Boolean = true,
        val checkingSSSSAction: Async<Unit> = Uninitialized,
        val step: Step = Step.ResetAll,
        val activeDeviceCount: Int = 0,
        val showResetAllAction: Boolean = false,
        val userId: String = "",
        val keyId: String?,
        val requestType: RequestType,
        val resultKeyStoreAlias: String
) : MavericksState {

    constructor(args: SharedSecureStorageActivity.Args) : this(
            keyId = args.keyId,
            requestType = if (args.writeSecrets.isNotEmpty()) {
                RequestType.WriteSecrets(args.writeSecrets)
            } else {
                RequestType.ReadSecrets(args.requestedSecrets)
            },
            resultKeyStoreAlias = args.resultKeyStoreAlias,
            step = args.currentStep,
    )

    enum class Step {
        EnterPassphrase,
        EnterKey,
        ResetAll
    }
}

class SharedSecureStorageViewModel @AssistedInject constructor(
        @Assisted private val initialState: SharedSecureStorageViewState,
        private val stringProvider: StringProvider,
        private val session: Session,
        private val matrix: Matrix,
) :
        VectorViewModel<SharedSecureStorageViewState, SharedSecureStorageAction, SharedSecureStorageViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SharedSecureStorageViewModel, SharedSecureStorageViewState> {
        override fun create(initialState: SharedSecureStorageViewState): SharedSecureStorageViewModel
    }

    init {
        setState {
            copy(userId = session.myUserId)
        }
        if (initialState.requestType is RequestType.ReadSecrets) {
            val integrityResult =
                    session.sharedSecretStorageService().checkShouldBeAbleToAccessSecrets(initialState.requestType.secretsName, initialState.keyId)
            if (integrityResult !is IntegrityResult.Success) {
                _viewEvents.post(
                        SharedSecureStorageViewEvent.Error(
                                stringProvider.getString(CommonStrings.enter_secret_storage_invalid),
                                true
                        )
                )
            }
        }

        if (initialState.step != SharedSecureStorageViewState.Step.ResetAll) {
            val keyResult = initialState.keyId?.let { session.sharedSecretStorageService().getKey(it) }
                    ?: session.sharedSecretStorageService().getDefaultKey()

            if (!keyResult.isSuccess()) {
                _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
            } else {
                val info = (keyResult as KeyInfoResult.Success).keyInfo
                if (info.content.passphrase != null) {
                    setState {
                        copy(
                                hasPassphrase = true,
                                ready = true,
                                step = SharedSecureStorageViewState.Step.EnterPassphrase
                        )
                    }
                } else {
                    setState {
                        copy(
                                hasPassphrase = false,
                                ready = true,
                                step = SharedSecureStorageViewState.Step.EnterKey
                        )
                    }
                }
            }
        } else {
            setState { copy(ready = true) }
        }

        session.flow()
                .liveUserCryptoDevices(session.myUserId)
                .distinctUntilChanged()
                .execute {
                    copy(
                            activeDeviceCount = it.invoke()?.size ?: 0
                    )
                }
    }

    override fun handle(action: SharedSecureStorageAction) = withState {
        when (action) {
            is SharedSecureStorageAction.Cancel -> handleCancel()
            is SharedSecureStorageAction.SubmitPassphrase -> handleSubmitPassphrase(action)
            SharedSecureStorageAction.UseKey -> handleUseKey()
            is SharedSecureStorageAction.SubmitKey -> handleSubmitKey(action)
            SharedSecureStorageAction.Back -> handleBack()
            SharedSecureStorageAction.ForgotResetAll -> handleResetAll()
            SharedSecureStorageAction.DoResetAll -> handleDoResetAll()
        }
    }

    private fun handleDoResetAll() {
        // as we are going to reset, we'd better cancel all outgoing requests
        // if not they could be accepted in the middle of the reset process
        // and cause strange use cases
        viewModelScope.launch {
            session.cryptoService().verificationService().getExistingVerificationRequests(session.myUserId).forEach {
                session.cryptoService().verificationService().cancelVerificationRequest(it)
            }
            _viewEvents.post(SharedSecureStorageViewEvent.ShowResetBottomSheet)
        }
    }

    private fun handleResetAll() {
        setState {
            copy(
                    step = SharedSecureStorageViewState.Step.ResetAll
            )
        }
    }

    private fun handleUseKey() {
        setState {
            copy(
                    step = SharedSecureStorageViewState.Step.EnterKey
            )
        }
    }

    private fun handleBack() = withState { state ->
        if (state.checkingSSSSAction is Loading) return@withState // ignore
        when (state.step) {
            SharedSecureStorageViewState.Step.EnterKey -> {
                if (state.hasPassphrase) {
                    setState {
                        copy(
                                step = SharedSecureStorageViewState.Step.EnterPassphrase
                        )
                    }
                } else {
                    _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
                }
            }
            /*
            SharedSecureStorageViewState.Step.ResetAll -> {
                setState {
                    copy(
                            step = if (state.hasPassphrase) SharedSecureStorageViewState.Step.EnterPassphrase
                            else SharedSecureStorageViewState.Step.EnterKey
                    )
                }
            }
             */
            else -> {
                _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
            }
        }
    }

    private fun handleSubmitKey(action: SharedSecureStorageAction.SubmitKey) {
        _viewEvents.post(SharedSecureStorageViewEvent.ShowModalLoading)
        val decryptedSecretMap = HashMap<String, String>()
        setState { copy(checkingSSSSAction = Loading()) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val recoveryKey = action.recoveryKey
                val keyInfoResult = session.sharedSecretStorageService().getDefaultKey()
                if (!keyInfoResult.isSuccess()) {
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    _viewEvents.post(SharedSecureStorageViewEvent.Error(stringProvider.getString(CommonStrings.failed_to_access_secure_storage)))
                    return@launch
                }
                val keyInfo = (keyInfoResult as KeyInfoResult.Success).keyInfo

                _viewEvents.post(
                        SharedSecureStorageViewEvent.UpdateLoadingState(
                                WaitingViewData(
                                        message = stringProvider.getString(CommonStrings.keys_backup_restoring_computing_key_waiting_message),
                                        isIndeterminate = true
                                )
                        )
                )
                val keySpec = RawBytesKeySpec.fromRecoveryKey(recoveryKey) ?: return@launch Unit.also {
                    _viewEvents.post(SharedSecureStorageViewEvent.KeyInlineError(stringProvider.getString(CommonStrings.bootstrap_invalid_recovery_key)))
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    setState {
                        copy(checkingSSSSAction = Fail(IllegalArgumentException(stringProvider.getString(CommonStrings.bootstrap_invalid_recovery_key))))
                    }
                }
                withContext(Dispatchers.IO) {
                    performRequest(keyInfo, keySpec, decryptedSecretMap)
                }
            }.fold({
                setState { copy(checkingSSSSAction = Success(Unit)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                val safeForIntentCypher = ByteArrayOutputStream().also {
                    it.use {
                        matrix.secureStorageService().securelyStoreObject(decryptedSecretMap as Map<String, String>, initialState.resultKeyStoreAlias, it)
                    }
                }.toByteArray().toBase64NoPadding()
                _viewEvents.post(SharedSecureStorageViewEvent.FinishSuccess(safeForIntentCypher))
            }, {
                setState { copy(checkingSSSSAction = Fail(it)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                _viewEvents.post(SharedSecureStorageViewEvent.KeyInlineError(stringProvider.getString(CommonStrings.keys_backup_passphrase_error_decrypt)))
            })
        }
    }

    private suspend fun performRequest(keyInfo: KeyInfo, keySpec: RawBytesKeySpec, decryptedSecretMap: HashMap<String, String>) {
        when (val requestType = initialState.requestType) {
            is RequestType.ReadSecrets -> {
                requestType.secretsName.forEach {
                    if (session.accountDataService().getUserAccountDataEvent(it) != null) {
                        val res = session.sharedSecretStorageService().getSecret(
                                name = it,
                                keyId = keyInfo.id,
                                secretKey = keySpec
                        )
                        decryptedSecretMap[it] = res
                    } else {
                        Timber.w("## Cannot find secret $it in SSSS, skip")
                    }
                }
            }
            is RequestType.WriteSecrets -> {
                requestType.secretsNameValue.forEach {
                    val (name, value) = it

                    session.sharedSecretStorageService().storeSecret(
                            name = name,
                            secretBase64 = value,
                            keys = listOf(KeyRef(keyInfo.id, keySpec))
                    )
                    decryptedSecretMap[name] = value
                }
            }
        }
    }

    private fun handleSubmitPassphrase(action: SharedSecureStorageAction.SubmitPassphrase) {
        _viewEvents.post(SharedSecureStorageViewEvent.ShowModalLoading)
        val decryptedSecretMap = HashMap<String, String>()
        setState { copy(checkingSSSSAction = Loading()) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val passphrase = action.passphrase
                val keyInfoResult = session.sharedSecretStorageService().getDefaultKey()
                if (!keyInfoResult.isSuccess()) {
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    _viewEvents.post(SharedSecureStorageViewEvent.Error("Cannot find ssss key"))
                    return@launch
                }
                val keyInfo = (keyInfoResult as KeyInfoResult.Success).keyInfo

                _viewEvents.post(
                        SharedSecureStorageViewEvent.UpdateLoadingState(
                                WaitingViewData(
                                        message = stringProvider.getString(CommonStrings.keys_backup_restoring_computing_key_waiting_message),
                                        isIndeterminate = true
                                )
                        )
                )
                val keySpec = RawBytesKeySpec.fromPassphrase(
                        passphrase,
                        keyInfo.content.passphrase?.salt ?: "",
                        keyInfo.content.passphrase?.iterations ?: 0,
                        object : ProgressListener {
                            override fun onProgress(progress: Int, total: Int) {
                                _viewEvents.post(
                                        SharedSecureStorageViewEvent.UpdateLoadingState(
                                                WaitingViewData(
                                                        message = stringProvider.getString(CommonStrings.keys_backup_restoring_computing_key_waiting_message),
                                                        isIndeterminate = false,
                                                        progress = progress,
                                                        progressTotal = total
                                                )
                                        )
                                )
                            }
                        }
                )

                withContext(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        performRequest(keyInfo, keySpec, decryptedSecretMap)
                    }
                }
            }.fold({
                setState { copy(checkingSSSSAction = Success(Unit)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                val safeForIntentCypher = ByteArrayOutputStream().also {
                    it.use {
                        matrix.secureStorageService().securelyStoreObject(decryptedSecretMap as Map<String, String>, initialState.resultKeyStoreAlias, it)
                    }
                }.toByteArray().toBase64NoPadding()
                _viewEvents.post(SharedSecureStorageViewEvent.FinishSuccess(safeForIntentCypher))
            }, {
                setState { copy(checkingSSSSAction = Fail(it)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                _viewEvents.post(SharedSecureStorageViewEvent.InlineError(stringProvider.getString(CommonStrings.keys_backup_passphrase_error_decrypt)))
            })
        }
    }

    private fun handleCancel() {
        _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
    }

    companion object : MavericksViewModelFactory<SharedSecureStorageViewModel, SharedSecureStorageViewState> by hiltMavericksViewModelFactory()
}
