/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.pin.lockscreen.ui

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.withState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.pin.lockscreen.biometrics.BiometricAuthError
import im.vector.app.features.pin.lockscreen.biometrics.BiometricHelper
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguratorProvider
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeysMigrator
import im.vector.app.features.pin.lockscreen.pincode.PinCodeHelper
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider

class LockScreenViewModel @AssistedInject constructor(
        @Assisted val initialState: LockScreenViewState,
        private val pinCodeHelper: PinCodeHelper,
        private val biometricHelper: BiometricHelper,
        private val lockScreenKeysMigrator: LockScreenKeysMigrator,
        private val configuratorProvider: LockScreenConfiguratorProvider,
        private val buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
) : VectorViewModel<LockScreenViewState, LockScreenAction, LockScreenViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LockScreenViewModel, LockScreenViewState> {
        override fun create(initialState: LockScreenViewState): LockScreenViewModel
    }

    companion object : MavericksViewModelFactory<LockScreenViewModel, LockScreenViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): LockScreenViewState {
            return LockScreenViewState(
                    lockScreenConfiguration = DUMMY_CONFIGURATION,
                    canUseBiometricAuth = false,
                    showBiometricPromptAutomatically = false,
                    pinCodeState = PinCodeState.Idle,
                    isBiometricKeyInvalidated = false,
            )
        }

        private val DUMMY_CONFIGURATION = LockScreenConfiguration(
                mode = LockScreenMode.VERIFY,
                pinCodeLength = 4,
                isStrongBiometricsEnabled = false,
                isDeviceCredentialUnlockEnabled = false,
                isWeakBiometricsEnabled = false,
                needsNewCodeValidation = false,
        )
    }

    private var firstEnteredCode: String? = null

    // BiometricPrompt will automatically disable system auth after too many failed auth attempts
    private var isSystemAuthTemporarilyDisabledByBiometricPrompt = false

    init {
        // We need this to run synchronously before we start reading the configurations
        runBlocking { lockScreenKeysMigrator.migrateIfNeeded() }

        configuratorProvider.configurationFlow
                .onEach { updateConfiguration(it) }
                .launchIn(viewModelScope)
    }

    override fun handle(action: LockScreenAction) {
        when (action) {
            is LockScreenAction.PinCodeEntered -> onPinCodeEntered(action.value)
            is LockScreenAction.ShowBiometricPrompt -> showBiometricPrompt(action.callingActivity)
        }
    }

    private fun onPinCodeEntered(code: String) = flow {
        val state = awaitState()
        when (state.lockScreenConfiguration.mode) {
            LockScreenMode.CREATE -> {
                if (firstEnteredCode == null && state.lockScreenConfiguration.needsNewCodeValidation) {
                    firstEnteredCode = code
                    _viewEvents.post(LockScreenViewEvent.ClearPinCode(false))
                    emit(PinCodeState.FirstCodeEntered)
                } else {
                    if (!state.lockScreenConfiguration.needsNewCodeValidation || code == firstEnteredCode) {
                        pinCodeHelper.createPinCode(code)
                        _viewEvents.post(LockScreenViewEvent.CodeCreationComplete)
                        emit(null)
                    } else {
                        firstEnteredCode = null
                        _viewEvents.post(LockScreenViewEvent.ClearPinCode(true))
                        emit(PinCodeState.Idle)
                    }
                }
            }
            LockScreenMode.VERIFY -> {
                if (pinCodeHelper.verifyPinCode(code)) {
                    _viewEvents.post(LockScreenViewEvent.AuthSuccessful(AuthMethod.PIN_CODE))
                    emit(null)
                } else {
                    _viewEvents.post(LockScreenViewEvent.AuthFailure(AuthMethod.PIN_CODE))
                    emit(null)
                }
            }
        }
    }.catch { error ->
        _viewEvents.post(LockScreenViewEvent.AuthError(AuthMethod.PIN_CODE, error))
    }.onEach { newPinState ->
        newPinState?.let { setState { copy(pinCodeState = it) } }
    }.launchIn(viewModelScope)

    @SuppressLint("NewApi")
    private fun showBiometricPrompt(activity: FragmentActivity) = flow {
        emitAll(biometricHelper.authenticate(activity))
    }.catch { error ->
        if (buildVersionSdkIntProvider.get() >= Build.VERSION_CODES.M && error is KeyPermanentlyInvalidatedException) {
            removeBiometricAuthentication()
        } else if (error is BiometricAuthError && error.isAuthDisabledError) {
            isSystemAuthTemporarilyDisabledByBiometricPrompt = true
            updateStateWithBiometricInfo()
        }
        _viewEvents.post(LockScreenViewEvent.AuthError(AuthMethod.BIOMETRICS, error))
    }.onEach { success ->
        _viewEvents.post(
                if (success) LockScreenViewEvent.AuthSuccessful(AuthMethod.BIOMETRICS)
                else LockScreenViewEvent.AuthFailure(AuthMethod.BIOMETRICS)
        )
    }.launchIn(viewModelScope)

    fun reset() {
        configuratorProvider.reset()
    }

    private fun removeBiometricAuthentication() {
        biometricHelper.disableAuthentication()
        updateStateWithBiometricInfo()
    }

    private fun updateStateWithBiometricInfo() {
        val configuration = withState(this) { it.lockScreenConfiguration }
        val canUseBiometricAuth = configuration.mode == LockScreenMode.VERIFY &&
                !isSystemAuthTemporarilyDisabledByBiometricPrompt &&
                biometricHelper.isSystemAuthEnabledAndValid
        val isBiometricKeyInvalidated = biometricHelper.hasSystemKey && !biometricHelper.isSystemKeyValid
        val showBiometricPromptAutomatically = canUseBiometricAuth &&
                configuration.autoStartBiometric
        setState {
            copy(
                    canUseBiometricAuth = canUseBiometricAuth,
                    showBiometricPromptAutomatically = showBiometricPromptAutomatically,
                    isBiometricKeyInvalidated = isBiometricKeyInvalidated
            )
        }
    }

    private fun updateConfiguration(configuration: LockScreenConfiguration) {
        setState { copy(lockScreenConfiguration = configuration) }
        updateStateWithBiometricInfo()
    }
}
