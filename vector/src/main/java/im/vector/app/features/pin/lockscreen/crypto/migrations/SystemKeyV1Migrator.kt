/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.os.Build
import androidx.annotation.RequiresApi
import im.vector.app.features.pin.lockscreen.crypto.KeyStoreCrypto
import im.vector.app.features.pin.lockscreen.di.BiometricKeyAlias
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber
import java.security.KeyStore
import javax.inject.Inject

/**
 * Migrates from the v1 of the biometric/system key to the new format, adding extra security measures to the new key.
 */
class SystemKeyV1Migrator @Inject constructor(
        @BiometricKeyAlias private val systemKeyAlias: String,
        private val keyStore: KeyStore,
        private val keystoreCryptoFactory: KeyStoreCrypto.Factory,
        private val vectorPreferences: VectorPreferences,
) {

    /**
     * Removes the old v1 system key and creates a new system key.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun migrate() {
        keyStore.deleteEntry(SYSTEM_KEY_ALIAS_V1)
        val systemKeyStoreCrypto = keystoreCryptoFactory.provide(systemKeyAlias, keyNeedsUserAuthentication = true)
        runCatching {
            systemKeyStoreCrypto.ensureKey()
        }.onFailure { e ->
            Timber.e(e, "Could not migrate v1 biometric key. Biometric authentication will be disabled.")
            systemKeyStoreCrypto.deleteKey()
            vectorPreferences.setUseBiometricToUnlock(false)
        }
    }

    /**
     * Checks if an entry with [SYSTEM_KEY_ALIAS_V1] exists in the [keyStore].
     */
    fun isMigrationNeeded() = keyStore.containsAlias(SYSTEM_KEY_ALIAS_V1)

    companion object {
        internal const val SYSTEM_KEY_ALIAS_V1 = "vector.system"
    }
}
