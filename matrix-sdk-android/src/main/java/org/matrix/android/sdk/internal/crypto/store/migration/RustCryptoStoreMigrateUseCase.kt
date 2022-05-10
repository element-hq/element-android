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

package org.matrix.android.sdk.internal.crypto.store.migration

import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.database.awaitTransaction
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import uniffi.olm.ProgressListener
import java.io.File
import javax.inject.Inject

internal class RustCryptoStoreMigrateUseCase @Inject constructor(
        @CryptoDatabase private val realmConfiguration: RealmConfiguration,
        @SessionFilesDirectory private val dataDir: File,
        private val extractMigrationData: ExtractMigrationDataUseCase) {

    suspend operator fun invoke(progressListener: ProgressListener) = runCatching {
        migrate(progressListener)
    }

    private suspend fun migrate(progressListener: ProgressListener) {
        awaitTransaction(realmConfiguration) { realm: Realm ->
            val migrationData = extractMigrationData(realm)
            uniffi.olm.migrate(migrationData, dataDir.path, null, progressListener)
        }
    }
}
