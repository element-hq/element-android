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

package im.vector.app.features.pin.lockscreen.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricPrompt
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import java.security.Key
import java.security.KeyStore

/**
 * Wrapper class to make working with KeyStore and keys easier.
 */
class KeyStoreCrypto @AssistedInject constructor(
        @Assisted val alias: String,
        @Assisted keyNeedsUserAuthentication: Boolean,
        context: Context,
        private val buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
        private val keyStore: KeyStore,
) {

    @AssistedFactory
    interface Factory {
        fun provide(alias: String, keyNeedsUserAuthentication: Boolean): KeyStoreCrypto
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var secretStoringUtils: SecretStoringUtils = SecretStoringUtils(context, keyStore, buildVersionSdkIntProvider, keyNeedsUserAuthentication)

    /**
     * Ensures a [Key] for the [alias] exists and validates it.
     * @throws KeyPermanentlyInvalidatedException if key is not valid.
     */
    @SuppressLint("NewApi")
    @Throws(KeyPermanentlyInvalidatedException::class)
    fun ensureKey() = secretStoringUtils.ensureKey(alias).also {
        // Check validity of Key by initializing an encryption Cipher
        secretStoringUtils.getEncryptCipher(alias)
    }

    /**
     * Encrypts the [ByteArray] value passed using generated the crypto key.
     */
    fun encrypt(value: ByteArray): ByteArray = secretStoringUtils.securelyStoreBytes(value, alias)

    /**
     * Encrypts the [String] value passed using generated the crypto key.
     */
    fun encrypt(value: String): ByteArray = encrypt(value.toByteArray())

    /**
     * Encrypts the [ByteArray] value passed using generated the crypto key.
     * @return A Base64 encoded String.
     */
    fun encryptToString(value: ByteArray): String = Base64.encodeToString(encrypt(value), Base64.NO_WRAP)

    /**
     * Encrypts the [String] value passed using generated the crypto key.
     * @return A Base64 encoded String.
     */
    fun encryptToString(value: String): String = Base64.encodeToString(encrypt(value), Base64.NO_WRAP)

    /**
     * Decrypts the [ByteArray] value passed using the generated crypto key.
     */
    fun decrypt(value: ByteArray): ByteArray = secretStoringUtils.loadSecureSecretBytes(value, alias)

    /**
     * Decrypts the [String] value passed using the generated crypto key.
     */
    fun decrypt(value: String): ByteArray = decrypt(Base64.decode(value, Base64.NO_WRAP))

    /**
     * Decrypts the [ByteArray] value passed using the generated crypto key.
     * @return The decrypted contents in as a String.
     */
    fun decryptToString(value: ByteArray): String = String(decrypt(value))

    /**
     * Decrypts the [String] value passed using the generated crypto key.
     * @return The decrypted contents in as a String.
     */
    fun decryptToString(value: String): String = String(decrypt(value))

    /**
     * Check if the key associated with the [alias] is valid.
     */
    @SuppressLint("NewApi")
    fun hasValidKey(): Boolean {
        val keyExists = hasKey()
        return if (buildVersionSdkIntProvider.get() >= Build.VERSION_CODES.M && keyExists) {
            val initializedKey = tryOrNull("Error validating lockscreen system key.") { ensureKey() }
            initializedKey != null
        } else {
            keyExists
        }
    }

    /**
     * Check if the key associated with the [alias] is stored in the [KeyStore].
     */
    fun hasKey(): Boolean = keyStore.containsAlias(alias)

    /**
     * Deletes the key associated with the [alias] from the [KeyStore].
     */
    fun deleteKey() = secretStoringUtils.safeDeleteKey(alias)

    /**
     * Creates a [BiometricPrompt.CryptoObject] to be used in authentication.
     * @throws KeyPermanentlyInvalidatedException if key is invalidated.
     */
    @Throws(KeyPermanentlyInvalidatedException::class)
    fun getAuthCryptoObject() = BiometricPrompt.CryptoObject(secretStoringUtils.getEncryptCipher(alias))
}
