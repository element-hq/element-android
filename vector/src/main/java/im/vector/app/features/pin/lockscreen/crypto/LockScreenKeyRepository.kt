/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import im.vector.app.features.pin.lockscreen.di.BiometricKeyAlias
import im.vector.app.features.pin.lockscreen.di.PinCodeKeyAlias
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class in charge of managing both the PIN code key and the system authentication keys.
 */
@Singleton
class LockScreenKeyRepository @Inject constructor(
        @PinCodeKeyAlias private val pinCodeKeyAlias: String,
        @BiometricKeyAlias private val systemKeyAlias: String,
        private val keyStoreCryptoFactory: KeyStoreCrypto.Factory,
) {

    private val pinCodeCrypto: KeyStoreCrypto by lazy {
        keyStoreCryptoFactory.provide(pinCodeKeyAlias, keyNeedsUserAuthentication = false)
    }
    private val systemKeyCrypto: KeyStoreCrypto by lazy {
        keyStoreCryptoFactory.provide(systemKeyAlias, keyNeedsUserAuthentication = true)
    }

    /**
     * Encrypts the [pinCode], creating the associated key if needed.
     */
    fun encryptPinCode(pinCode: String): String = pinCodeCrypto.encryptToString(pinCode)

    /**
     * Decrypts the [encodedPinCode] into a plain [String] or null.
     */
    fun decryptPinCode(encodedPinCode: String): String = pinCodeCrypto.decryptToString(encodedPinCode)

    /**
     * Get the key associated to the system authentication (biometrics). It will be created if it didn't exist before.
     * Note: this key will be invalidated by new biometric enrollments.
     * @throws KeyPermanentlyInvalidatedException if key is invalidated.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun ensureSystemKey() = systemKeyCrypto.ensureKey()

    /**
     * Returns if the PIN code key already exists.
     */
    fun hasPinCodeKey() = pinCodeCrypto.hasKey()

    /**
     * Returns if the system authentication key already exists.
     */
    fun hasSystemKey() = systemKeyCrypto.hasKey()

    /**
     * Deletes the PIN code key from the KeyStore.
     */
    fun deletePinCodeKey() = pinCodeCrypto.deleteKey()

    /**
     * Deletes the system authentication key from the KeyStore.
     */
    fun deleteSystemKey() = systemKeyCrypto.deleteKey()

    /**
     * Checks if the current system authentication key exists and is valid.
     */
    fun isSystemKeyValid() = systemKeyCrypto.hasValidKey()

    /**
     * Returns a [BiometricPrompt.CryptoObject] for the system key.
     */
    fun getSystemKeyAuthCryptoObject() = systemKeyCrypto.getAuthCryptoObject()
}
