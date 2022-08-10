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
import android.os.Build
import im.vector.app.features.pin.lockscreen.crypto.migrations.LegacyPinCodeMigrator
import im.vector.app.features.pin.lockscreen.crypto.migrations.MissingSystemKeyMigrator
import im.vector.app.features.pin.lockscreen.crypto.migrations.SystemKeyV1Migrator
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import javax.inject.Inject

/**
 * Performs all migrations needed for the lock screen keys.
 */
class LockScreenKeysMigrator @Inject constructor(
        private val legacyPinCodeMigrator: LegacyPinCodeMigrator,
        private val missingSystemKeyMigrator: MissingSystemKeyMigrator,
        private val systemKeyV1Migrator: SystemKeyV1Migrator,
        private val versionProvider: BuildVersionSdkIntProvider,
) {
    /**
     * Performs any needed migrations in order.
     */
    @SuppressLint("NewApi")
    suspend fun migrateIfNeeded() {
        if (legacyPinCodeMigrator.isMigrationNeeded()) {
            legacyPinCodeMigrator.migrate()
            missingSystemKeyMigrator.migrateIfNeeded()
        }

        if (systemKeyV1Migrator.isMigrationNeeded() && versionProvider.get() >= Build.VERSION_CODES.M) {
            systemKeyV1Migrator.migrate()
        }
    }
}
