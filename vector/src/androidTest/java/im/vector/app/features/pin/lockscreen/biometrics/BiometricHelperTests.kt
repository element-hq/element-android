/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.biometrics

import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.TestBuildVersionSdkIntProvider
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.app.features.pin.lockscreen.crypto.LockScreenCryptoConstants
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeyRepository
import im.vector.app.features.pin.lockscreen.tests.LockScreenTestActivity
import im.vector.app.features.pin.lockscreen.ui.fallbackprompt.FallbackBiometricDialogFragment
import im.vector.app.features.pin.lockscreen.utils.DevicePromptCheck
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.coInvoking
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BiometricHelperTests {

    private val biometricManager = mockk<BiometricManager>(relaxed = true)
    private val lockScreenKeyRepository = mockk<LockScreenKeyRepository>(relaxed = true)
    private val buildVersionSdkIntProvider = TestBuildVersionSdkIntProvider()
    private val keyStore = KeyStore.getInstance(LockScreenCryptoConstants.ANDROID_KEY_STORE).also { it.load(null) }
    private val secretStoringUtils = SecretStoringUtils(
            InstrumentationRegistry.getInstrumentation().targetContext,
            keyStore,
            buildVersionSdkIntProvider,
            false,
    )

    @Before
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun canUseWeakBiometricAuthReturnsTrueIfIsFaceUnlockEnabledAndCanAuthenticate() {
        every { biometricManager.canAuthenticate(BIOMETRIC_WEAK) } returns BIOMETRIC_SUCCESS
        val configuration = createDefaultConfiguration(isFaceUnlockEnabled = true)
        val biometricUtils = createBiometricHelper(configuration)

        biometricUtils.canUseWeakBiometricAuth.shouldBeTrue()

        val biometricUtilsWithDisabledAuth = createBiometricHelper(createDefaultConfiguration(isFaceUnlockEnabled = false))
        biometricUtilsWithDisabledAuth.canUseWeakBiometricAuth.shouldBeFalse()

        every { biometricManager.canAuthenticate(BIOMETRIC_WEAK) } returns BIOMETRIC_ERROR_NONE_ENROLLED
        biometricUtils.canUseWeakBiometricAuth.shouldBeFalse()
    }

    @Test
    fun canUseStrongBiometricAuthReturnsTrueIfIsBiometricsEnabledAndCanAuthenticate() {
        every { biometricManager.canAuthenticate(BIOMETRIC_STRONG) } returns BIOMETRIC_SUCCESS
        val configuration = createDefaultConfiguration(isBiometricsEnabled = true)
        val biometricUtils = createBiometricHelper(configuration)

        biometricUtils.canUseStrongBiometricAuth.shouldBeTrue()

        val biometricUtilsWithDisabledAuth = createBiometricHelper(createDefaultConfiguration(isBiometricsEnabled = false))
        biometricUtilsWithDisabledAuth.canUseStrongBiometricAuth.shouldBeFalse()

        every { biometricManager.canAuthenticate(BIOMETRIC_STRONG) } returns BIOMETRIC_ERROR_NONE_ENROLLED
        biometricUtils.canUseStrongBiometricAuth.shouldBeFalse()
    }

    @Test
    fun canUseDeviceCredentialAuthReturnsTrueIfIsDeviceCredentialsUnlockEnabledAndCanAuthenticate() {
        every { biometricManager.canAuthenticate(DEVICE_CREDENTIAL) } returns BIOMETRIC_SUCCESS
        val configuration = createDefaultConfiguration(isDeviceCredentialUnlockEnabled = true)
        val biometricUtils = createBiometricHelper(configuration)

        biometricUtils.canUseDeviceCredentialsAuth.shouldBeTrue()

        val biometricUtilsWithDisabledAuth = createBiometricHelper(createDefaultConfiguration(isDeviceCredentialUnlockEnabled = false))
        biometricUtilsWithDisabledAuth.canUseDeviceCredentialsAuth.shouldBeFalse()

        every { biometricManager.canAuthenticate(DEVICE_CREDENTIAL) } returns BIOMETRIC_ERROR_NONE_ENROLLED
        biometricUtils.canUseDeviceCredentialsAuth.shouldBeFalse()
    }

    @Test
    fun isSystemAuthEnabledReturnsTrueIfAnyAuthenticationMethodIsAvailableAndEnabledAndSystemKeyExists() {
        val biometricHelper = mockk<BiometricHelper>(relaxed = true) {
            every { hasSystemKey } returns true
            every { isSystemKeyValid } returns true
            every { canUseAnySystemAuth } answers { callOriginal() }
            every { isSystemAuthEnabledAndValid } answers { callOriginal() }
        }
        biometricHelper.isSystemAuthEnabledAndValid.shouldBeFalse()

        every { biometricHelper.canUseWeakBiometricAuth } returns true
        biometricHelper.isSystemAuthEnabledAndValid.shouldBeTrue()

        every { biometricHelper.canUseWeakBiometricAuth } returns false
        every { biometricHelper.canUseStrongBiometricAuth } returns true
        biometricHelper.isSystemAuthEnabledAndValid.shouldBeTrue()

        every { biometricHelper.canUseStrongBiometricAuth } returns false
        every { biometricHelper.canUseDeviceCredentialsAuth } returns true
        biometricHelper.isSystemAuthEnabledAndValid.shouldBeTrue()

        every { biometricHelper.isSystemKeyValid } returns false
        biometricHelper.isSystemAuthEnabledAndValid.shouldBeFalse()
    }

    @Test
    fun hasSystemKeyReturnsKeyHelperHasSystemKey() {
        val biometricUtils = createBiometricHelper(createDefaultConfiguration())
        every { lockScreenKeyRepository.hasSystemKey() } returns true
        biometricUtils.hasSystemKey.shouldBeTrue()

        every { lockScreenKeyRepository.hasSystemKey() } returns false
        biometricUtils.hasSystemKey.shouldBeFalse()
    }

    @Test
    fun isSystemKeyValidReturnsKeyHelperIsSystemKeyValid() {
        val biometricUtils = createBiometricHelper(createDefaultConfiguration())
        every { lockScreenKeyRepository.isSystemKeyValid() } returns true
        biometricUtils.isSystemKeyValid.shouldBeTrue()

        every { lockScreenKeyRepository.isSystemKeyValid() } returns false
        biometricUtils.isSystemKeyValid.shouldBeFalse()
    }

    @Test
    fun disableAuthenticationDeletesSystemKeyAndCancelsPrompt() {
        val biometricUtils = spyk(createBiometricHelper(createDefaultConfiguration()))
        biometricUtils.disableAuthentication()

        verify { lockScreenKeyRepository.deleteSystemKey() }
        verify { biometricUtils.cancelPrompt() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore("This won't work in CI as the emulator won't have biometric auth enabled.")
    @Test
    fun authenticateShowsPrompt() = runTest {
        val biometricUtils = createBiometricHelper(createDefaultConfiguration(isBiometricsEnabled = true))
        every { lockScreenKeyRepository.isSystemKeyValid() } returns true
        val latch = CountDownLatch(1)
        with(ActivityScenario.launch(LockScreenTestActivity::class.java)) {
            onActivity { activity ->
                biometricUtils.authenticate(activity)
                activity.supportFragmentManager.fragments.isNotEmpty().shouldBeTrue()
                close()
                latch.countDown()
            }
        }
        latch.await(1, TimeUnit.SECONDS)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R) // Due to some issues with mockk and CryptoObject initialization
    @Test
    fun authenticateInDeviceWithIssuesShowsFallbackPromptDialog() = runTest {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M
        mockkStatic("kotlinx.coroutines.flow.FlowKt")
        val mockAuthChannel: Channel<Boolean> = mockk(relaxed = true) {
            // Empty flow to keep the dialog open
            every { receiveAsFlow() } returns flowOf()
        }
        val biometricUtils = spyk(createBiometricHelper(createDefaultConfiguration(isBiometricsEnabled = true))) {
            every { createAuthChannel() } returns mockAuthChannel
        }
        mockkObject(DevicePromptCheck)
        every { DevicePromptCheck.isDeviceWithNoBiometricUI } returns true
        every { lockScreenKeyRepository.isSystemKeyValid() } returns true

        val keyAlias = UUID.randomUUID().toString()
        every { biometricUtils.getAuthCryptoObject() } returns BiometricPrompt.CryptoObject(secretStoringUtils.getEncryptCipher(keyAlias))
        val latch = CountDownLatch(1)
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, LockScreenTestActivity::class.java)
        with(ActivityScenario.launch<LockScreenTestActivity>(intent)) {
            onActivity { activity ->
                biometricUtils.authenticate(activity)
                launch {
                    activity.supportFragmentManager.fragments.any { it is FallbackBiometricDialogFragment }.shouldBeTrue()
                    close()
                    latch.countDown()
                }
            }
        }
        latch.await(1, TimeUnit.SECONDS)
        keyStore.deleteEntry(keyAlias)
        unmockkObject(DevicePromptCheck)
        unmockkStatic("kotlinx.coroutines.flow.FlowKt")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R) // Due to some issues with mockk and CryptoObject initialization
    fun enableAuthenticationDeletesSystemKeyOnFailure() = runTest {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M
        val mockAuthChannel = Channel<Boolean>(capacity = 1)
        val biometricUtils = spyk(createBiometricHelper(createDefaultConfiguration(isBiometricsEnabled = true))) {
            every { createAuthChannel() } returns mockAuthChannel
            every { authenticateWithPromptInternal(any(), any(), any()) } returns mockk()
        }
        justRun { lockScreenKeyRepository.deleteSystemKey() }

        val latch = CountDownLatch(1)
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, LockScreenTestActivity::class.java)
        ActivityScenario.launch<LockScreenTestActivity>(intent).onActivity { activity ->
            activity.lifecycleScope.launch {
                val exception = IllegalStateException("Some error")
                launch {
                    mockAuthChannel.close(exception)
                }
                coInvoking { biometricUtils.enableAuthentication(activity).collect() } shouldThrow exception
                latch.countDown()
            }
        }

        latch.await(1, TimeUnit.SECONDS)
        verify { lockScreenKeyRepository.deleteSystemKey() }
    }

    private fun createBiometricHelper(configuration: LockScreenConfiguration): BiometricHelper {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return BiometricHelper(configuration, context, lockScreenKeyRepository, biometricManager, buildVersionSdkIntProvider)
    }

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
