/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto

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
    suspend fun migrateIfNeeded() {
        if (legacyPinCodeMigrator.isMigrationNeeded()) {
            legacyPinCodeMigrator.migrate()
            missingSystemKeyMigrator.migrateIfNeeded()
        }

        if (systemKeyV1Migrator.isMigrationNeeded() && versionProvider.isAtLeast(Build.VERSION_CODES.M)) {
            systemKeyV1Migrator.migrate()
        }
    }
}
