/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.fragment

import android.app.KeyguardManager
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import im.vector.app.features.pin.lockscreen.biometrics.BiometricAuthError
import im.vector.app.features.pin.lockscreen.biometrics.BiometricHelper
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeysMigrator
import im.vector.app.features.pin.lockscreen.pincode.PinCodeHelper
import im.vector.app.features.pin.lockscreen.ui.AuthMethod
import im.vector.app.features.pin.lockscreen.ui.LockScreenAction
import im.vector.app.features.pin.lockscreen.ui.LockScreenViewEvent
import im.vector.app.features.pin.lockscreen.ui.LockScreenViewModel
import im.vector.app.features.pin.lockscreen.ui.LockScreenViewState
import im.vector.app.features.pin.lockscreen.ui.PinCodeState
import im.vector.app.test.TestBuildVersionSdkIntProvider
import im.vector.app.test.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LockScreenViewModelTests {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val pinCodeHelper = mockk<PinCodeHelper>(relaxed = true)
    private val biometricHelper = mockk<BiometricHelper>(relaxed = true)
    private val biometricHelperFactory = object : BiometricHelper.BiometricHelperFactory {
        override fun create(configuration: LockScreenConfiguration): BiometricHelper {
            return biometricHelper
        }
    }
    private val keysMigrator = mockk<LockScreenKeysMigrator>(relaxed = true)
    private val keyguardManager = mockk<KeyguardManager>(relaxed = true) {
        every { isDeviceLocked } returns false
    }
    private val versionProvider = TestBuildVersionSdkIntProvider()

    @Before
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `init migrates old keys to new ones if needed`() {
        // given
        val initialState = createViewState()
        // when
        LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        // then
        coVerify { keysMigrator.migrateIfNeeded() }
    }

    @Test
    fun `init updates the initial state with biometric info`() = runTest {
        // given
        every { biometricHelper.isSystemAuthEnabledAndValid } returns true
        val initialState = createViewState()
        // when
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        // then
        val newState = viewModel.awaitState() // Can't use viewModel.test() here since we want to record events emitted on init
        newState shouldNotBeEqualTo initialState
    }

    @Test
    fun `Updating the initial state with biometric info waits until device is unlocked on Android 12+`() = runTest {
        // given
        val initialState = createViewState()
        versionProvider.value = Build.VERSION_CODES.S
        // when
        LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        // then
        verify { keyguardManager.isDeviceLocked }
    }

    @Test
    fun `when ViewModel is instantiated initialState is updated with biometric info`() {
        // given
        givenShowBiometricPromptAutomatically()
        val initialState = createViewState()
        // when
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        // then
        withState(viewModel) { newState ->
            initialState shouldNotBeEqualTo newState
        }
    }

    @Test
    fun `when onPinCodeEntered is called in VERIFY mode and verification is successful, code is verified and result is emitted as a ViewEvent`() = runTest {
        // given
        val initialState = createViewState()
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        coEvery { pinCodeHelper.verifyPinCode(any()) } returns true
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        // then
        coVerify { pinCodeHelper.verifyPinCode(any()) }
        test.assertEvents(LockScreenViewEvent.AuthSuccessful(AuthMethod.PIN_CODE))
        test.assertStates(initialState)
    }

    @Test
    fun `when onPinCodeEntered is called in VERIFY mode and verification fails, the error result is emitted as a ViewEvent`() = runTest {
        // given
        val initialState = createViewState()
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        coEvery { pinCodeHelper.verifyPinCode(any()) } returns false
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        // then
        coVerify { pinCodeHelper.verifyPinCode(any()) }
        test.assertEvents(LockScreenViewEvent.AuthFailure(AuthMethod.PIN_CODE))
        test.assertStates(initialState)
    }

    @Test
    fun `when onPinCodeEntered is called in CREATE mode with no confirmation needed it creates the pin code`() = runTest {
        // given
        val configuration = createDefaultConfiguration(mode = LockScreenMode.CREATE, needsNewCodeValidation = false)
        val initialState = createViewState(lockScreenConfiguration = configuration)
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        // then
        coVerify { pinCodeHelper.createPinCode(any()) }
        test.assertEvents(LockScreenViewEvent.CodeCreationComplete)
    }

    @Test
    fun `when onPinCodeEntered is called twice in CREATE mode with confirmation needed it verifies and creates the pin code`() = runTest {
        // given
        val pinCode = "1234"
        val configuration = createDefaultConfiguration(mode = LockScreenMode.CREATE, needsNewCodeValidation = true)
        val initialState = createViewState(lockScreenConfiguration = configuration, pinCodeState = PinCodeState.FirstCodeEntered(pinCode))
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.PinCodeEntered(pinCode))
        // then
        test.assertEvents(LockScreenViewEvent.CodeCreationComplete)
                .assertLatestState { (it.pinCodeState as? PinCodeState.FirstCodeEntered)?.pinCode == pinCode }
    }

    @Test
    fun `when onPinCodeEntered is called in CREATE mode with incorrect confirmation it clears the pin code`() = runTest {
        // given
        val configuration = createDefaultConfiguration(mode = LockScreenMode.CREATE, needsNewCodeValidation = true)
        val initialState = createViewState(lockScreenConfiguration = configuration, pinCodeState = PinCodeState.FirstCodeEntered("1234"))
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.PinCodeEntered("4321"))
        // then
        test.assertEvents(LockScreenViewEvent.ClearPinCode(true))
                .assertLatestState(initialState.copy(pinCodeState = PinCodeState.Idle))
    }

    @Test
    fun `onPinCodeEntered handles exceptions`() = runTest {
        // given
        val initialState = createViewState()
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val exception = IllegalStateException("Something went wrong")
        coEvery { pinCodeHelper.verifyPinCode(any()) } throws exception
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        // then
        test.assertEvents(LockScreenViewEvent.AuthError(AuthMethod.PIN_CODE, exception))
    }

    @Test
    fun `when showBiometricPrompt catches a KeyPermanentlyInvalidatedException it disables biometric authentication`() = runTest {
        // given
        versionProvider.value = Build.VERSION_CODES.M
        every { biometricHelper.isSystemKeyValid } returns false
        val exception = KeyPermanentlyInvalidatedException()
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } throws exception
        val configuration = createDefaultConfiguration(mode = LockScreenMode.VERIFY, needsNewCodeValidation = true, isBiometricsEnabled = true)
        val initialState = createViewState(
                canUseBiometricAuth = true,
                isBiometricKeyInvalidated = false,
                lockScreenConfiguration = configuration
        )
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))
        // then
        test.assertEvents(LockScreenViewEvent.ShowBiometricKeyInvalidatedMessage)
                // Biometric key is invalidated so biometric auth is disabled
                .assertLatestState { !it.canUseBiometricAuth }
        verify { biometricHelper.disableAuthentication() }
    }

    @Test
    fun `when showBiometricPrompt receives an event it propagates it as a ViewEvent`() = runTest {
        // given
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } returns flowOf(false, true)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))
        // then
        test.assertEvents(LockScreenViewEvent.AuthFailure(AuthMethod.BIOMETRICS), LockScreenViewEvent.AuthSuccessful(AuthMethod.BIOMETRICS))
    }

    @Test
    fun `showBiometricPrompt handles exceptions`() = runTest {
        // given
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val exception = IllegalStateException("Something went wrong")
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } throws exception
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))
        // then
        test.assertEvents(LockScreenViewEvent.AuthError(AuthMethod.BIOMETRICS, exception))
    }

    @Test
    fun `when showBiometricPrompt handles isAuthDisabledError, canUseBiometricAuth becomes false`() = runTest {
        // given
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val exception = BiometricAuthError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT, "Permanent lockout")
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } throws exception
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))
        // then
        exception.isAuthDisabledError.shouldBeTrue()
        test.assertEvents(LockScreenViewEvent.AuthError(AuthMethod.BIOMETRICS, exception))
                .assertLatestState { !it.canUseBiometricAuth }
    }

    @Test
    fun `when OnUIReady action is received and showBiometricPromptAutomatically is true it shows prompt`() = runTest {
        // given
        givenShowBiometricPromptAutomatically()
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.OnUIReady)
        // then
        test.assertEvents(LockScreenViewEvent.ShowBiometricPromptAutomatically)
    }

    @Test
    fun `when OnUIReady action is received and isBiometricKeyInvalidated is true it shows prompt`() = runTest {
        // given
        givenBiometricKeyIsInvalidated()
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelperFactory, keysMigrator, versionProvider, keyguardManager)
        val test = viewModel.test()
        // when
        viewModel.handle(LockScreenAction.OnUIReady)
        // then
        test.assertEvents(LockScreenViewEvent.ShowBiometricKeyInvalidatedMessage)
    }

    private fun createViewState(
            lockScreenConfiguration: LockScreenConfiguration = createDefaultConfiguration(),
            canUseBiometricAuth: Boolean = false,
            showBiometricPromptAutomatically: Boolean = false,
            pinCodeState: PinCodeState = PinCodeState.Idle,
            isBiometricKeyInvalidated: Boolean = false,
    ): LockScreenViewState = LockScreenViewState(
            lockScreenConfiguration, canUseBiometricAuth, showBiometricPromptAutomatically, pinCodeState, isBiometricKeyInvalidated
    )

    private fun createDefaultConfiguration(
            mode: LockScreenMode = LockScreenMode.VERIFY,
            pinCodeLength: Int = 4,
            isBiometricsEnabled: Boolean = false,
            isFaceUnlockEnabled: Boolean = false,
            isDeviceCredentialUnlockEnabled: Boolean = false,
            needsNewCodeValidation: Boolean = false,
            otherChanges: LockScreenConfiguration.() -> LockScreenConfiguration = { this },
    ): LockScreenConfiguration = LockScreenConfiguration(
            mode,
            pinCodeLength,
            isBiometricsEnabled,
            isFaceUnlockEnabled,
            isDeviceCredentialUnlockEnabled,
            needsNewCodeValidation
    ).let(otherChanges)

    private fun givenBiometricKeyIsInvalidated() {
        every { biometricHelper.hasSystemKey } returns true
        every { biometricHelper.isSystemKeyValid } returns false
    }

    private fun givenShowBiometricPromptAutomatically() {
        every { biometricHelper.isSystemAuthEnabledAndValid } returns true
    }
}
