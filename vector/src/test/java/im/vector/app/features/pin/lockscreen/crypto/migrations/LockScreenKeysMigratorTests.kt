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

import android.os.Build
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeysMigrator
import im.vector.app.test.TestBuildVersionSdkIntProvider
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LockScreenKeysMigratorTests {

    private val legacyPinCodeMigrator = mockk<LegacyPinCodeMigrator>(relaxed = true)
    private val missingSystemKeyMigrator = mockk<MissingSystemKeyMigrator>(relaxed = true)
    private val systemKeyV1Migrator = mockk<SystemKeyV1Migrator>(relaxed = true)
    private val versionProvider = TestBuildVersionSdkIntProvider()
    private val migrator = LockScreenKeysMigrator(legacyPinCodeMigrator, missingSystemKeyMigrator, systemKeyV1Migrator, versionProvider)

    @Test
    fun `When legacy pin code migration is needed, both legacyPinCodeMigrator and missingSystemKeyMigrator will be run`() {
        // When no migration is needed
        every { legacyPinCodeMigrator.isMigrationNeeded() } returns false

        runBlocking { migrator.migrateIfNeeded() }

        coVerify(exactly = 0) { legacyPinCodeMigrator.migrate() }
        verify(exactly = 0) { missingSystemKeyMigrator.migrateIfNeeded() }

        // When migration is needed
        every { legacyPinCodeMigrator.isMigrationNeeded() } returns true

        runBlocking { migrator.migrateIfNeeded() }

        coVerify { legacyPinCodeMigrator.migrate() }
        verify { missingSystemKeyMigrator.migrateIfNeeded() }
    }

    @Test
    fun `System key from v1 migration will not be run for versions that don't support biometrics`() {
        versionProvider.value = Build.VERSION_CODES.LOLLIPOP
        every { systemKeyV1Migrator.isMigrationNeeded() } returns true

        runBlocking { migrator.migrateIfNeeded() }

        verify(exactly = 0) { systemKeyV1Migrator.migrate() }
    }

    @Test
    fun `When system key from v1 migration is needed it will be run`() {
        versionProvider.value = Build.VERSION_CODES.M
        every { systemKeyV1Migrator.isMigrationNeeded() } returns false

        runBlocking { migrator.migrateIfNeeded() }

        verify(exactly = 0) { systemKeyV1Migrator.migrate() }

        every { systemKeyV1Migrator.isMigrationNeeded() } returns true

        runBlocking { migrator.migrateIfNeeded() }

        verify { systemKeyV1Migrator.migrate() }
    }
}
