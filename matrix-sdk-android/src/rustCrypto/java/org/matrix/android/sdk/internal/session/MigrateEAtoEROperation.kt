/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session

import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.crypto.store.db.migration.rust.ExtractMigrationDataUseCase
import org.matrix.rustcomponents.sdk.crypto.ProgressListener
import timber.log.Timber
import java.io.File

class MigrateEAtoEROperation {

    fun execute(cryptoRealm: RealmConfiguration, sessionFilesDir: File): File {
        // Temporary code for migration
        if (!sessionFilesDir.exists()) {
            sessionFilesDir.mkdir()
            // perform a migration?
            val extractMigrationData = ExtractMigrationDataUseCase()
            val hasExitingData = extractMigrationData.hasExistingData(cryptoRealm)
            if (!hasExitingData) return sessionFilesDir

            try {
                val progressListener = object : ProgressListener {
                    override fun onProgress(progress: Int, total: Int) {
                        Timber.v("OnProgress: $progress/$total")
                    }
                }

                Realm.getInstance(cryptoRealm).use { realm ->
                    val migrationData = extractMigrationData.extractData(realm)
                    org.matrix.rustcomponents.sdk.crypto.migrate(migrationData, sessionFilesDir.path, null, progressListener)
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Failure while calling rust migration method")
                throw failure
            }
        }
        return sessionFilesDir
    }
}