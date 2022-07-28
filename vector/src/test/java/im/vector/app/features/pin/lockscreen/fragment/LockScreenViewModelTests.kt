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

package im.vector.app.features.pin.lockscreen.fragment

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.test.MvRxTestRule
import com.airbnb.mvrx.withState
import im.vector.app.features.pin.lockscreen.biometrics.BiometricHelper
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguratorProvider
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
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LockScreenViewModelTests {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val pinCodeHelper = mockk<PinCodeHelper>(relaxed = true)
    private val biometricHelper = mockk<BiometricHelper>(relaxed = true)
    private val keysMigrator = mockk<LockScreenKeysMigrator>(relaxed = true)
    private val versionProvider = TestBuildVersionSdkIntProvider()

    @Before
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `init migrates old keys to new ones if needed`() {
        val initialState = createViewState()
        val configProvider = LockScreenConfiguratorProvider(createDefaultConfiguration())
        LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)

        coVerify { keysMigrator.migrateIfNeeded() }
    }

    @Test
    fun `when ViewModel is instantiated initialState is updated with biometric info`() {
        val initialState = createViewState()
        val configProvider = LockScreenConfiguratorProvider(createDefaultConfiguration())
        // This should set canUseBiometricAuth to true
        every { biometricHelper.isSystemAuthEnabledAndValid } returns true
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)
        val newState = withState(viewModel) { it }
        initialState shouldNotBeEqualTo newState
    }

    @Test
    fun `when onPinCodeEntered is called in VERIFY mode, the code is verified and the result is emitted as a ViewEvent`() = runTest {
        val initialState = createViewState()
        val configProvider = LockScreenConfiguratorProvider(createDefaultConfiguration())
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)
        coEvery { pinCodeHelper.verifyPinCode(any()) } returns true

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        val stateBefore = viewModel.awaitState()

        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        coVerify { pinCodeHelper.verifyPinCode(any()) }
        events.assertValues(LockScreenViewEvent.AuthSuccessful(AuthMethod.PIN_CODE))

        coEvery { pinCodeHelper.verifyPinCode(any()) } returns false
        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        events.assertValues(LockScreenViewEvent.AuthSuccessful(AuthMethod.PIN_CODE), LockScreenViewEvent.AuthFailure(AuthMethod.PIN_CODE))

        val stateAfter = viewModel.awaitState()
        stateBefore shouldBeEqualTo stateAfter
    }

    @Test
    fun `when onPinCodeEntered is called in CREATE mode with no confirmation needed it creates the pin code`() = runTest {
        val configuration = createDefaultConfiguration(mode = LockScreenMode.CREATE, needsNewCodeValidation = false)
        val initialState = createViewState(lockScreenConfiguration = configuration)
        val configProvider = LockScreenConfiguratorProvider(configuration)
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        coVerify { pinCodeHelper.createPinCode(any()) }

        events.assertValues(LockScreenViewEvent.CodeCreationComplete)
    }

    @Test
    fun `when onPinCodeEntered is called twice in CREATE mode with confirmation needed it verifies and creates the pin code`() = runTest {
        val configuration = createDefaultConfiguration(mode = LockScreenMode.CREATE, needsNewCodeValidation = true)
        val configProvider = LockScreenConfiguratorProvider(configuration)
        val initialState = createViewState(lockScreenConfiguration = configuration)
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))

        events.assertValues(LockScreenViewEvent.ClearPinCode(false))
        val pinCodeState = viewModel.awaitState().pinCodeState
        pinCodeState shouldBeEqualTo PinCodeState.FirstCodeEntered

        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))
        events.assertValues(LockScreenViewEvent.ClearPinCode(false), LockScreenViewEvent.CodeCreationComplete)
    }

    @Test
    fun `when onPinCodeEntered is called in CREATE mode with incorrect confirmation it clears the pin code`() = runTest {
        val configuration = createDefaultConfiguration(mode = LockScreenMode.CREATE, needsNewCodeValidation = true)
        val initialState = createViewState(lockScreenConfiguration = configuration)
        val configProvider = LockScreenConfiguratorProvider(configuration)
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))

        events.assertValues(LockScreenViewEvent.ClearPinCode(false))
        val pinCodeState = viewModel.awaitState().pinCodeState
        pinCodeState shouldBeEqualTo PinCodeState.FirstCodeEntered

        viewModel.handle(LockScreenAction.PinCodeEntered("4321"))
        events.assertValues(LockScreenViewEvent.ClearPinCode(false), LockScreenViewEvent.ClearPinCode(true))
        val newPinCodeState = viewModel.awaitState().pinCodeState
        newPinCodeState shouldBeEqualTo PinCodeState.Idle
    }

    @Test
    fun `onPinCodeEntered handles exceptions`() = runTest {
        val initialState = createViewState()
        val configProvider = LockScreenConfiguratorProvider(createDefaultConfiguration())
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)
        val exception = IllegalStateException("Something went wrong")
        coEvery { pinCodeHelper.verifyPinCode(any()) } throws exception

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.PinCodeEntered("1234"))

        events.assertValues(LockScreenViewEvent.AuthError(AuthMethod.PIN_CODE, exception))
    }

    @Test
    fun `when showBiometricPrompt catches a KeyPermanentlyInvalidatedException it disables biometric authentication`() = runTest {
        versionProvider.value = Build.VERSION_CODES.M

        every { biometricHelper.isSystemAuthEnabledAndValid } returns true
        every { biometricHelper.isSystemKeyValid } returns true
        val exception = KeyPermanentlyInvalidatedException()
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } throws exception
        coEvery { biometricHelper.disableAuthentication() } coAnswers {
            every { biometricHelper.isSystemAuthEnabledAndValid } returns false
        }
        val configuration = createDefaultConfiguration(mode = LockScreenMode.VERIFY, needsNewCodeValidation = true, isBiometricsEnabled = true)
        val configProvider = LockScreenConfiguratorProvider(configuration)
        val initialState = createViewState(
                canUseBiometricAuth = true,
                isBiometricKeyInvalidated = false,
                lockScreenConfiguration = configuration
        )
        val viewModel = LockScreenViewModel(initialState, pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))

        events.assertValues(LockScreenViewEvent.AuthError(AuthMethod.BIOMETRICS, exception))
        verify { biometricHelper.disableAuthentication() }

        // System key was deleted, biometric auth should be disabled
        val newState = viewModel.awaitState()
        newState.canUseBiometricAuth.shouldBeFalse()
    }

    @Test
    fun `when showBiometricPrompt receives an event it propagates it as a ViewEvent`() = runTest {
        val configProvider = LockScreenConfiguratorProvider(createDefaultConfiguration())
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } returns flowOf(false, true)

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))

        events.assertValues(LockScreenViewEvent.AuthFailure(AuthMethod.BIOMETRICS), LockScreenViewEvent.AuthSuccessful(AuthMethod.BIOMETRICS))
    }

    @Test
    fun `showBiometricPrompt handles exceptions`() = runTest {
        val configProvider = LockScreenConfiguratorProvider(createDefaultConfiguration())
        val viewModel = LockScreenViewModel(createViewState(), pinCodeHelper, biometricHelper, keysMigrator, configProvider, versionProvider)
        val exception = IllegalStateException("Something went wrong")
        coEvery { biometricHelper.authenticate(any<FragmentActivity>()) } throws exception

        val events = viewModel.test().viewEvents
        events.assertNoValues()

        viewModel.handle(LockScreenAction.ShowBiometricPrompt(mockk()))

        events.assertValues(LockScreenViewEvent.AuthError(AuthMethod.BIOMETRICS, exception))
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
}
