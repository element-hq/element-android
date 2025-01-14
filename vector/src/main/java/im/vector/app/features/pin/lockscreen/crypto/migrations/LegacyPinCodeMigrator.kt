/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.os.Build
import android.util.Base64
import androidx.annotation.VisibleForTesting
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.lockscreen.crypto.LockScreenCryptoConstants.LEGACY_PIN_CODE_KEY_ALIAS
import im.vector.app.features.pin.lockscreen.di.PinCodeKeyAlias
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.inject.Inject

/**
 * Used to migrate from the old PIN code key ciphers to a more secure ones.
 */
class LegacyPinCodeMigrator @Inject constructor(
        @PinCodeKeyAlias private val pinCodeKeyAlias: String,
        private val pinCodeStore: PinCodeStore,
        private val keyStore: KeyStore,
        private val secretStoringUtils: SecretStoringUtils,
        private val buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
) {

    private val legacyKey: Key get() = keyStore.getKey(LEGACY_PIN_CODE_KEY_ALIAS, null)

    /**
     * Migrates from the old ciphers and renames [LEGACY_PIN_CODE_KEY_ALIAS] to [pinCodeKeyAlias].
     */
    suspend fun migrate() {
        if (!keyStore.containsAlias(LEGACY_PIN_CODE_KEY_ALIAS)) return

        val pinCode = getDecryptedPinCode() ?: return
        val encryptedBytes = secretStoringUtils.securelyStoreBytes(pinCode.toByteArray(), pinCodeKeyAlias)
        val encryptedPinCode = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        pinCodeStore.savePinCode(encryptedPinCode)
        keyStore.deleteEntry(LEGACY_PIN_CODE_KEY_ALIAS)
    }

    fun isMigrationNeeded(): Boolean = keyStore.containsAlias(LEGACY_PIN_CODE_KEY_ALIAS)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun getDecryptedPinCode(): String? {
        val encryptedPinCode = pinCodeStore.getPinCode() ?: return null
        val cipher = getDecodeCipher()
        val bytes = cipher.doFinal(Base64.decode(encryptedPinCode, Base64.NO_WRAP))
        return String(bytes)
    }

    private fun getDecodeCipher(): Cipher {
        return when (buildVersionSdkIntProvider.get()) {
            Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.LOLLIPOP_MR1 -> getCipherL()
            else -> getCipherM()
        }.also { it.init(Cipher.DECRYPT_MODE, legacyKey) }
    }

    private fun getCipherL(): Cipher {
        // We cannot mock this in tests as it's tied to the actual cryptographic implementation of the OS version
        val provider = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) "AndroidOpenSSL" else "AndroidKeyStoreBCWorkaround"
        val transformation = "RSA/ECB/PKCS1Padding"
        return Cipher.getInstance(transformation, provider)
    }

    private fun getCipherM(): Cipher {
        val transformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        return Cipher.getInstance(transformation)
    }
}
