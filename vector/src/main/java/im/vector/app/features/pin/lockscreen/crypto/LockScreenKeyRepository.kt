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

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.RequiresApi
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber

/**
 * Class in charge of managing both the PIN code key and the system authentication keys.
 */
class LockScreenKeyRepository(
        baseName: String,
        private val pinCodeMigrator: PinCodeMigrator,
        private val vectorPreferences: VectorPreferences,
        private val keyStoreCryptoFactory: KeyStoreCrypto.Factory,
) {

    private val pinCodeKeyAlias = "$baseName.pin_code"
    private val systemKeyAlias = "$baseName.system"

    private val pinCodeCrypto: KeyStoreCrypto by lazy {
        keyStoreCryptoFactory.provide(pinCodeKeyAlias,  keyNeedsUserAuthentication = false)
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
     * Migrates the PIN code key and encrypted value to use a more secure cipher, also creates a new system key if needed.
     */
    suspend fun migrateKeysIfNeeded() {
        if (pinCodeMigrator.isMigrationNeeded()) {
            pinCodeMigrator.migrate(pinCodeKeyAlias)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && vectorPreferences.useBiometricsToUnlock()) {
                try {
                    ensureSystemKey()
                } catch (e: KeyPermanentlyInvalidatedException) {
                    Timber.e("Could not automatically create biometric key.", e)
                }
            }
        }
    }
}
