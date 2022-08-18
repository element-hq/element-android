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

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.annotation.SuppressLint
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
    @SuppressLint("NewApi")
    fun migrateIfNeeded() {
        if (buildVersionSdkIntProvider.get() >= Build.VERSION_CODES.M && vectorPreferences.useBiometricsToUnlock()) {
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
