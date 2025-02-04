/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.biometrics

import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeyRepository
import im.vector.app.features.pin.lockscreen.ui.fallbackprompt.FallbackBiometricDialogFragment
import im.vector.app.features.pin.lockscreen.utils.DevicePromptCheck
import im.vector.app.features.pin.lockscreen.utils.hasFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import java.security.KeyStore
import javax.crypto.Cipher
import kotlin.coroutines.CoroutineContext

/**
 * This is a helper to manage system authentication (biometric and other types) and the system key.
 */
class BiometricHelper @AssistedInject constructor(
        @Assisted private val configuration: LockScreenConfiguration,
        @ApplicationContext private val context: Context,
        private val lockScreenKeyRepository: LockScreenKeyRepository,
        private val biometricManager: BiometricManager,
        private val buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
) {
    private var prompt: BiometricPrompt? = null

    @AssistedFactory
    interface BiometricHelperFactory {
        fun create(configuration: LockScreenConfiguration): BiometricHelper
    }

    /**
     * Returns true if a weak biometric method (i.e.: some face or iris unlock implementations) can be used.
     */
    val canUseWeakBiometricAuth: Boolean
        get() = configuration.isWeakBiometricsEnabled && biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS

    /**
     * Returns true if a strong biometric method (i.e.: fingerprint, some face or iris unlock implementations) can be used.
     */
    val canUseStrongBiometricAuth: Boolean
        get() = configuration.isStrongBiometricsEnabled && biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS

    /**
     * Returns true if the device credentials can be used to unlock (system pin code, password, pattern, etc.).
     */
    val canUseDeviceCredentialsAuth: Boolean
        get() = configuration.isDeviceCredentialUnlockEnabled && biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS

    /**
     * Returns true if any system authentication method (biometric weak/strong or device credentials) can be used.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    internal val canUseAnySystemAuth: Boolean
        get() = canUseWeakBiometricAuth || canUseStrongBiometricAuth || canUseDeviceCredentialsAuth

    /**
     * Returns true if any system authentication method and there is a valid associated key.
     */
    val isSystemAuthEnabledAndValid: Boolean get() = canUseAnySystemAuth && isSystemKeyValid

    /**
     * Returns true is the [KeyStore] contains a key associated to system authentication.
     */
    val hasSystemKey: Boolean get() = lockScreenKeyRepository.hasSystemKey()

    /**
     * Returns true if the system key is valid, that is, not invalidated by new enrollments.
     */
    val isSystemKeyValid: Boolean get() = lockScreenKeyRepository.isSystemKeyValid()

    /**
     * Enables system authentication after displaying a [BiometricPrompt] in the passed [FragmentActivity].
     * Note: Must be called from the Main thread.
     * @return: A [Flow] with the [Boolean] success/failure result or a [BiometricAuthError].
     */
    @MainThread
    fun enableAuthentication(activity: FragmentActivity): Flow<Boolean> {
        return authenticateInternal(activity, checkSystemKeyExists = false, cryptoObject = getAuthCryptoObject())
    }

    /**
     * Disables system authentication cancelling the current [BiometricPrompt] if needed.
     * Note: Must be called from the Main thread.
     */
    @MainThread
    fun disableAuthentication() {
        lockScreenKeyRepository.deleteSystemKey()
        cancelPrompt()
    }

    /**
     * Displays a [BiometricPrompt] in the passed [FragmentActivity] and unlocking the system key if succeeds.
     * Note: Must be called from the Main thread.
     * @return: A [Flow] with the [Boolean] success/failure result or a [BiometricAuthError].
     */
    @MainThread
    fun authenticate(activity: FragmentActivity): Flow<Boolean> {
        return authenticateInternal(activity, checkSystemKeyExists = true, cryptoObject = getAuthCryptoObject())
    }

    /**
     * Displays a [BiometricPrompt] in the passed [Fragment] and unlocking the system key if succeeds.
     * Note: Must be called from the Main thread.
     * @return: A [Flow] with the [Boolean] success/failure result or a [BiometricAuthError].
     */
    @MainThread
    fun authenticate(fragment: Fragment): Flow<Boolean> {
        val activity = fragment.activity ?: return flowOf(false)
        return authenticate(activity)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun authenticateInternal(
            activity: FragmentActivity,
            checkSystemKeyExists: Boolean,
            cryptoObject: BiometricPrompt.CryptoObject,
    ): Flow<Boolean> {
        if (checkSystemKeyExists && !isSystemAuthEnabledAndValid) return flowOf(false)

        if (prompt != null) {
            cancelPrompt()
        }

        val channel = createAuthChannel()
        prompt = authenticateWithPromptInternal(activity, cryptoObject, channel)
        return flow {
            // We need to listen to the channel until it's closed
            while (!channel.isClosedForReceive) {
                val result = channel.receiveCatching()
                when (val exception = result.exceptionOrNull()) {
                    null -> result.getOrNull()?.let { emit(it) }
                    else -> {
                        // Exception found:
                        // 1. Stop collecting.
                        // 2. Remove the system key if we were creating it.
                        // 3. Throw the exception and remove the prompt reference
                        if (!checkSystemKeyExists) {
                            lockScreenKeyRepository.deleteSystemKey()
                        }
                        prompt = null
                        throw exception
                    }
                }
            }
            // Channel is closed, remove prompt reference
            prompt = null
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun authenticateWithPromptInternal(
            activity: FragmentActivity,
            cryptoObject: BiometricPrompt.CryptoObject,
            channel: Channel<Boolean>,
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = createSuspendingAuthCallback(channel, executor.asCoroutineDispatcher())
        val authenticators = getAvailableAuthenticators()
        val isUsingDeviceCredentialAuthenticator = authenticators.hasFlag(DEVICE_CREDENTIAL)
        val cancelButtonTitle = configuration.biometricCancelButtonTitle ?: context.getString(im.vector.lib.ui.styles.R.string.lockscreen_cancel)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(configuration.biometricTitle ?: context.getString(im.vector.lib.ui.styles.R.string.lockscreen_sign_in))
                .apply {
                    configuration.biometricSubtitle?.let {
                        setSubtitle(it)
                    }
                    if (!isUsingDeviceCredentialAuthenticator) {
                        setNegativeButtonText(cancelButtonTitle)
                    }
                }
                .setAllowedAuthenticators(authenticators)
                .build()

        return BiometricPrompt(activity, executor, callback).also { prompt ->
            showFallbackFragmentIfNeeded(activity, channel.receiveAsFlow(), executor.asCoroutineDispatcher()) {
                // For some reason this seems to be needed unless we want to receive a fragment transaction exception
                delay(1L)
                prompt.authenticate(promptInfo, cryptoObject)
            }
        }
    }

    private fun getAvailableAuthenticators(): Int {
        var authenticators = 0
        // Android 10 (Q) and below can only use a single authenticator at the same time
        if (buildVersionSdkIntProvider.get() <= Build.VERSION_CODES.Q) {
            authenticators = when {
                canUseStrongBiometricAuth -> BIOMETRIC_STRONG
                canUseWeakBiometricAuth -> BIOMETRIC_WEAK
                canUseDeviceCredentialsAuth -> DEVICE_CREDENTIAL
                else -> 0
            }
        } else {
            if (canUseDeviceCredentialsAuth) {
                authenticators += DEVICE_CREDENTIAL
            }
            if (canUseStrongBiometricAuth) {
                authenticators += BIOMETRIC_STRONG
            }
            // We can't use BIOMETRIC_STRONG and BIOMETRIC_WEAK at the same time. We should prioritize BIOMETRIC_STRONG.
            if (!authenticators.hasFlag(BIOMETRIC_STRONG) && canUseWeakBiometricAuth) {
                authenticators += BIOMETRIC_WEAK
            }
        }
        return authenticators
    }

    private fun createSuspendingAuthCallback(
            channel: Channel<Boolean>,
            coroutineContext: CoroutineContext,
    ): BiometricPrompt.AuthenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        private val scope = CoroutineScope(coroutineContext)
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            // Error is a terminal event, should close both the Channel and the CoroutineScope to free resources.
            channel.close(BiometricAuthError(errorCode, errString.toString()))
            scope.cancel()
        }

        override fun onAuthenticationFailed() {
            scope.launch { channel.send(false) }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val cipher = result.cryptoObject?.cipher
            if (isCipherValid(cipher)) {
                scope.launch {
                    channel.send(true)
                    // Success is a terminal event, should close both the Channel and the CoroutineScope to free resources.
                    channel.close()
                    scope.cancel()
                }
            } else {
                channel.close(IllegalStateException("System key was not valid after authentication."))
                scope.cancel()
            }
        }

        private fun isCipherValid(cipher: Cipher?): Boolean {
            if (cipher == null) return false
            return runCatching {
                cipher.doFinal("biometric_challenge".toByteArray())
            }.isSuccess
        }
    }

    /**
     * This method displays a fallback biometric prompt dialog for devices with issues with their system implementations.
     * @param activity [FragmentActivity] to display this fallback fragment in.
     * @param authenticationFLow [Flow] where the authentication events will be received.
     * @param coroutineContext [CoroutineContext] to run async code. It's shared with the [BiometricPrompt] executor value.
     * @param showPrompt Lambda containing the code to show the original [BiometricPrompt] above the fallback dialog.
     * @see [DevicePromptCheck].
     */
    private fun showFallbackFragmentIfNeeded(
            activity: FragmentActivity,
            authenticationFLow: Flow<Boolean>,
            coroutineContext: CoroutineContext,
            showPrompt: suspend () -> Unit
    ) {
        val scope = CoroutineScope(coroutineContext)
        if (DevicePromptCheck.isDeviceWithNoBiometricUI) {
            val fallbackFragment = activity.supportFragmentManager.findFragmentByTag(FALLBACK_BIOMETRIC_FRAGMENT_TAG) as? FallbackBiometricDialogFragment
                    ?: FallbackBiometricDialogFragment.instantiate(
                            title = configuration.biometricTitle,
                            description = configuration.biometricSubtitle,
                            cancelActionText = configuration.biometricCancelButtonTitle,
                    )
            fallbackFragment.onDismiss = { cancelPrompt() }
            fallbackFragment.authenticationFlow = authenticationFLow

            val transaction = activity.supportFragmentManager.beginTransaction()
                    .runOnCommit { scope.launch { showPrompt() } }
            fallbackFragment.show(transaction, FALLBACK_BIOMETRIC_FRAGMENT_TAG)
        } else {
            scope.launch { showPrompt() }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun cancelPrompt() {
        prompt?.cancelAuthentication()
        prompt = null
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun createAuthChannel(): Channel<Boolean> = Channel(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun getAuthCryptoObject(): BiometricPrompt.CryptoObject = lockScreenKeyRepository.getSystemKeyAuthCryptoObject()

    companion object {
        private const val FALLBACK_BIOMETRIC_FRAGMENT_TAG = "fragment.biometric_fallback"
    }
}
