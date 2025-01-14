/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui

import android.app.KeyguardManager
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.pin.lockscreen.biometrics.BiometricAuthError
import im.vector.app.features.pin.lockscreen.biometrics.BiometricHelper
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeysMigrator
import im.vector.app.features.pin.lockscreen.pincode.PinCodeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LockScreenViewModel @AssistedInject constructor(
        @Assisted val initialState: LockScreenViewState,
        private val pinCodeHelper: PinCodeHelper,
        biometricHelperFactory: BiometricHelper.BiometricHelperFactory,
        private val lockScreenKeysMigrator: LockScreenKeysMigrator,
        private val versionProvider: BuildVersionSdkIntProvider,
        private val keyguardManager: KeyguardManager,
) : VectorViewModel<LockScreenViewState, LockScreenAction, LockScreenViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LockScreenViewModel, LockScreenViewState> {
        override fun create(initialState: LockScreenViewState): LockScreenViewModel
    }

    companion object : MavericksViewModelFactory<LockScreenViewModel, LockScreenViewState> by hiltMavericksViewModelFactory()

    private val biometricHelper = biometricHelperFactory.create(initialState.lockScreenConfiguration)

    // BiometricPrompt will automatically disable system auth after too many failed auth attempts
    private var isSystemAuthTemporarilyDisabledByBiometricPrompt = false

    init {
        viewModelScope.launch {
            // Wait until the keyguard is unlocked before performing migrations, it might cause crashes otherwise on Android 12 and 12L
            waitUntilKeyguardIsUnlocked()
            // Migrate pin code / system keys if needed
            lockScreenKeysMigrator.migrateIfNeeded()
            // Update initial state with biometric info
            updateStateWithBiometricInfo()
        }
    }

    private fun observeStateChanges() {
        // The first time the state allows it, show the biometric prompt
        viewModelScope.launch {
            if (stateFlow.firstOrNull { it.showBiometricPromptAutomatically } != null) {
                _viewEvents.post(LockScreenViewEvent.ShowBiometricPromptAutomatically)
            }
        }

        // The first time the state allows it, react to biometric key being invalidated
        viewModelScope.launch {
            if (stateFlow.firstOrNull { it.isBiometricKeyInvalidated } != null) {
                onBiometricKeyInvalidated()
            }
        }
    }

    override fun handle(action: LockScreenAction) {
        when (action) {
            is LockScreenAction.PinCodeEntered -> onPinCodeEntered(action.value)
            is LockScreenAction.ShowBiometricPrompt -> showBiometricPrompt(action.callingActivity)
            is LockScreenAction.OnUIReady -> observeStateChanges()
        }
    }

    private fun onPinCodeEntered(code: String) = flow {
        val state = awaitState()
        when (state.lockScreenConfiguration.mode) {
            LockScreenMode.CREATE -> {
                val enteredPinCode = (state.pinCodeState as? PinCodeState.FirstCodeEntered)?.pinCode
                if (enteredPinCode == null && state.lockScreenConfiguration.needsNewCodeValidation) {
                    _viewEvents.post(LockScreenViewEvent.ClearPinCode(confirmationFailed = false))
                    emit(PinCodeState.FirstCodeEntered(code))
                } else {
                    if (!state.lockScreenConfiguration.needsNewCodeValidation || code == enteredPinCode) {
                        pinCodeHelper.createPinCode(code)
                        _viewEvents.post(LockScreenViewEvent.CodeCreationComplete)
                        emit(null)
                    } else {
                        _viewEvents.post(LockScreenViewEvent.ClearPinCode(confirmationFailed = true))
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
        if (newPinState != null) {
            setState { copy(pinCodeState = newPinState) }
        }
    }.launchIn(viewModelScope)

    private fun showBiometricPrompt(activity: FragmentActivity) = flow {
        emitAll(biometricHelper.authenticate(activity))
    }.catch { error ->
        when {
            versionProvider.isAtLeast(Build.VERSION_CODES.M) &&
                    error is KeyPermanentlyInvalidatedException -> {
                onBiometricKeyInvalidated()
            }
            else -> {
                if (error is BiometricAuthError && error.isAuthDisabledError) {
                    isSystemAuthTemporarilyDisabledByBiometricPrompt = true
                    updateStateWithBiometricInfo()
                }
                _viewEvents.post(LockScreenViewEvent.AuthError(AuthMethod.BIOMETRICS, error))
            }
        }
    }.onEach { success ->
        _viewEvents.post(
                if (success) LockScreenViewEvent.AuthSuccessful(AuthMethod.BIOMETRICS)
                else LockScreenViewEvent.AuthFailure(AuthMethod.BIOMETRICS)
        )
    }.launchIn(viewModelScope)

    private suspend fun onBiometricKeyInvalidated() {
        biometricHelper.disableAuthentication()
        updateStateWithBiometricInfo()
        _viewEvents.post(LockScreenViewEvent.ShowBiometricKeyInvalidatedMessage)
    }

    private suspend fun updateStateWithBiometricInfo() {
        // This is a terrible hack, but I found no other way to ensure this would be called only after the device is considered unlocked on Android 12+
        waitUntilKeyguardIsUnlocked()
        setState {
            val isBiometricKeyInvalidated = biometricHelper.hasSystemKey && !biometricHelper.isSystemKeyValid
            val canUseBiometricAuth = lockScreenConfiguration.mode == LockScreenMode.VERIFY &&
                    !isSystemAuthTemporarilyDisabledByBiometricPrompt &&
                    biometricHelper.isSystemAuthEnabledAndValid
            val showBiometricPromptAutomatically = canUseBiometricAuth && lockScreenConfiguration.autoStartBiometric
            copy(
                    canUseBiometricAuth = canUseBiometricAuth,
                    showBiometricPromptAutomatically = showBiometricPromptAutomatically,
                    isBiometricKeyInvalidated = isBiometricKeyInvalidated
            )
        }
    }

    /**
     * Wait until the device is unlocked. There seems to be a behavior change on Android 12 that makes [KeyguardManager.isDeviceLocked] return `false` even
     * after an Activity's `onResume` method. If we mix that with the system keys needing the device to be unlocked before they're used, we get crashes.
     * See issue [#6768](https://github.com/element-hq/element-android/issues/6768).
     */
    private suspend fun waitUntilKeyguardIsUnlocked() {
        if (versionProvider.isAtLeast(Build.VERSION_CODES.S)) {
            withTimeoutOrNull(5.seconds) {
                while (keyguardManager.isDeviceLocked) {
                    delay(50.milliseconds)
                }
            }
        }
    }
}
