/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.os.Build
import im.vector.app.features.pin.lockscreen.crypto.KeyStoreCrypto
import im.vector.app.features.pin.lockscreen.di.BiometricKeyAlias
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates a new system/biometric key when migrating from the old PFLockScreen implementation.
 */
class MissingSystemKeyMigrator @Inject constructor(
        @BiometricKeyAlias private val systemKeyAlias: String,
        private val keystoreCryptoFactory: KeyStoreCrypto.Factory,
        private val vectorPreferences: VectorPreferences,
        private val buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
) {

    /**
     * If user had biometric auth enabled, ensure system key exists, creating one if needed.
     */
    fun migrateIfNeeded() {
        if (buildVersionSdkIntProvider.isAtLeast(Build.VERSION_CODES.M) &&
                vectorPreferences.useBiometricsToUnlock()) {
            val systemKeyStoreCrypto = keystoreCryptoFactory.provide(systemKeyAlias, true)
            runCatching {
                systemKeyStoreCrypto.ensureKey()
            }.onFailure { e ->
                Timber.e(e, "Could not automatically create biometric key. Biometric authentication will be disabled.")
                systemKeyStoreCrypto.deleteKey()
                vectorPreferences.setUseBiometricToUnlock(false)
            }
        }
    }
}
