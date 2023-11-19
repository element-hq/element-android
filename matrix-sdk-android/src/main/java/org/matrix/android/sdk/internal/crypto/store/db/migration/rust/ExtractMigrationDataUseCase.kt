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

package org.matrix.android.sdk.internal.crypto.store.db.migration.rust

import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.olm.OlmUtility
import org.matrix.rustcomponents.sdk.crypto.MigrationData
import timber.log.Timber
import kotlin.system.measureTimeMillis

internal class ExtractMigrationDataUseCase(private val migrateGroupSessions: Boolean = false) {

    fun extractData(realm: RealmToMigrate, importPartial: ((MigrationData) -> Unit)) {
        return try {
            extract(realm, importPartial)
        } catch (failure: Throwable) {
            throw ExtractMigrationDataFailure(failure)
        }
    }

    fun hasExistingData(realmConfiguration: RealmConfiguration): Boolean {
        return Realm.getInstance(realmConfiguration).use { realm ->
            !realm.isEmpty &&
                    // Check if there is a MetaData object
                    realm.where<CryptoMetadataEntity>().count() > 0 &&
                    realm.where<CryptoMetadataEntity>().findFirst()?.olmAccountData != null
        }
    }

    private fun extract(realm: RealmToMigrate, importPartial: ((MigrationData) -> Unit)) {
        val pickleKey = OlmUtility.getRandomKey()

        val baseExtract = realm.getPickledAccount(pickleKey)
        // import the account asap
        importPartial(baseExtract)

        val chunkSize = 500
        realm.trackedUsersChunk(500) {
            importPartial(
                    baseExtract.copy(trackedUsers = it)
            )
        }

        var migratedOlmSessionCount = 0
        var writeTime = 0L
        measureTimeMillis {
            realm.pickledOlmSessions(pickleKey, chunkSize) { pickledSessions ->
                migratedOlmSessionCount += pickledSessions.size
                measureTimeMillis {
                    importPartial(
                            baseExtract.copy(sessions = pickledSessions)
                    )
                }.also { writeTime += it }
            }
        }.also {
            Timber.i("Migration: took $it ms to migrate $migratedOlmSessionCount olm sessions")
            Timber.i("Migration: rust import time $writeTime")
        }

        // We don't migrate outbound session by default directly after migration
        // We are going to do it lazyly when decryption fails
        if (migrateGroupSessions) {
            var migratedInboundGroupSessionCount = 0
            measureTimeMillis {
                realm.pickledOlmGroupSessions(pickleKey, chunkSize) { pickledSessions ->
                    migratedInboundGroupSessionCount += pickledSessions.size
                    measureTimeMillis {
                        importPartial(
                                baseExtract.copy(inboundGroupSessions = pickledSessions)
                        )
                    }.also { writeTime += it }
                }
            }.also {
                Timber.i("Migration: took $it ms to migrate $migratedInboundGroupSessionCount group sessions")
                Timber.i("Migration: rust import time $writeTime")
            }
        }
    }
}
