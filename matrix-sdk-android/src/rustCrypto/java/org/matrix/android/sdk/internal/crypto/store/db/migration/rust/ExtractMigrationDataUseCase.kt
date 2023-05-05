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
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.olm.OlmSession
import org.matrix.olm.OlmUtility
import org.matrix.rustcomponents.sdk.crypto.CrossSigningKeyExport
import org.matrix.rustcomponents.sdk.crypto.MigrationData
import org.matrix.rustcomponents.sdk.crypto.PickledAccount
import org.matrix.rustcomponents.sdk.crypto.PickledInboundGroupSession
import org.matrix.rustcomponents.sdk.crypto.PickledSession
import timber.log.Timber
import java.nio.charset.Charset
import kotlin.system.measureTimeMillis

private val charset = Charset.forName("UTF-8")

internal class ExtractMigrationDataUseCase(val migrateGroupSessions: Boolean = false) {

    fun extractData(realm: Realm, importPartial: ((MigrationData) -> Unit)) {
        return try {
            extract(realm, importPartial)
        } catch (failure: Throwable) {
            throw ExtractMigrationDataFailure
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

    private fun extract(realm: Realm, importPartial: ((MigrationData) -> Unit)) {
        val metadataEntity = realm.where<CryptoMetadataEntity>().findFirst()
                ?: throw java.lang.IllegalArgumentException("Rust db migration: No existing metadataEntity")

        val pickleKey = OlmUtility.getRandomKey()

        val masterKey = metadataEntity.xSignMasterPrivateKey
        val userKey = metadataEntity.xSignUserPrivateKey
        val selfSignedKey = metadataEntity.xSignSelfSignedPrivateKey

        val userId = metadataEntity.userId
                ?: throw java.lang.IllegalArgumentException("Rust db migration: userId is null")
        val deviceId = metadataEntity.deviceId
                ?: throw java.lang.IllegalArgumentException("Rust db migration: deviceID is null")

        val backupVersion = metadataEntity.backupVersion
        val backupRecoveryKey = metadataEntity.keyBackupRecoveryKey

        val isOlmAccountShared = metadataEntity.deviceKeysSentToServer

        val olmAccount = metadataEntity.getOlmAccount()
                ?: throw java.lang.IllegalArgumentException("Rust db migration: No existing account")
        val pickledOlmAccount = olmAccount.pickle(pickleKey, StringBuffer()).asString()
        olmAccount.oneTimeKeys()
        val pickledAccount = PickledAccount(
                userId = userId,
                deviceId = deviceId,
                pickle = pickledOlmAccount,
                shared = isOlmAccountShared,
                uploadedSignedKeyCount = 50
        )

        val baseExtract = MigrationData(
                account = pickledAccount,
                pickleKey = pickleKey.map { it.toUByte() },
                crossSigning = CrossSigningKeyExport(
                        masterKey = masterKey,
                        selfSigningKey = selfSignedKey,
                        userSigningKey = userKey
                ),
                sessions = emptyList(),
                backupRecoveryKey = backupRecoveryKey,
                trackedUsers = emptyList(),
                inboundGroupSessions = emptyList(),
                backupVersion = backupVersion,
                // TODO import room settings from legacy DB
                roomSettings = emptyMap()
        )
        // import the account asap
        importPartial(baseExtract)

        val chunkSize = 500
        realm.where<UserEntity>()
                .findAll()
                .chunked(chunkSize) { chunk ->
                    val trackedUserIds = chunk.mapNotNull { it.userId }
                    importPartial(
                            baseExtract.copy(trackedUsers = trackedUserIds)
                    )
                }

        var migratedOlmSessionCount = 0
        var readTime = 0L
        var writeTime = 0L
        measureTimeMillis {
            realm.where<OlmSessionEntity>().findAll()
                    .chunked(chunkSize) { chunk ->
                        migratedOlmSessionCount += chunk.size
                        val export: List<PickledSession>
                        measureTimeMillis {
                            export = chunk.map { it.toPickledSession(pickleKey) }
                        }.also {
                            readTime += it
                        }
                        measureTimeMillis {
                            importPartial(
                                    baseExtract.copy(sessions = export)
                            )
                        }.also { writeTime += it }
                    }
        }.also {
            Timber.i("Migration: took $it ms to migrate $migratedOlmSessionCount olm sessions")
            Timber.i("Migration: extract time $readTime")
            Timber.i("Migration: rust import time $writeTime")
        }

        // We don't migrate outbound session by default directly after migration
        // We are going to do it lazyly when decryption fails
        if (migrateGroupSessions) {
            var migratedInboundGroupSessionCount = 0
            readTime = 0
            writeTime = 0
            measureTimeMillis {
                realm.where<OlmInboundGroupSessionEntity>()
                        .findAll()
                        .chunked(chunkSize) { chunk ->
                            val export: List<PickledInboundGroupSession>
                            measureTimeMillis {
                                export = chunk.mapNotNull { it.toPickledInboundGroupSession(pickleKey) }
                            }.also {
                                readTime += it
                            }
                            migratedInboundGroupSessionCount += export.size
                            measureTimeMillis {
                                importPartial(
                                        baseExtract.copy(inboundGroupSessions = export)
                                )
                            }.also {
                                writeTime += it
                            }
                        }
            }.also {
                Timber.i("Migration: took $it ms to migrate $migratedInboundGroupSessionCount group sessions")
                Timber.i("Migration: extract time $readTime")
                Timber.i("Migration: rust import time $writeTime")
            }
        }

//        return baseExtract
    }

    private fun OlmInboundGroupSessionEntity.toPickledInboundGroupSession(pickleKey: ByteArray): PickledInboundGroupSession? {
        val senderKey = this.senderKey ?: return null
        val backedUp = this.backedUp
        val olmInboundGroupSession = this.getOlmGroupSession() ?: return null.also {
            Timber.w("Rust db migration: Failed to migrated group session $sessionId")
        }
        val data = this.getData() ?: return null.also {
            Timber.w("Rust db migration: Failed to migrated group session $sessionId, no meta data")
        }
        val roomId = data.roomId ?: return null.also {
            Timber.w("Rust db migration: Failed to migrated group session $sessionId, no roomId")
        }
        val pickledInboundGroupSession = olmInboundGroupSession.pickle(pickleKey, StringBuffer()).asString()
        return PickledInboundGroupSession(
                pickle = pickledInboundGroupSession,
                senderKey = senderKey,
                signingKey = data.keysClaimed.orEmpty(),
                roomId = roomId,
                forwardingChains = data.forwardingCurve25519KeyChain.orEmpty(),
                imported = data.trusted.orFalse().not(),
                backedUp = backedUp
        )
    }

    private fun OlmSessionEntity.toPickledSession(pickleKey: ByteArray): PickledSession {
        val deviceKey = this.deviceKey ?: ""
        val lastReceivedMessageTs = this.lastReceivedMessageTs
        val olmSessionStr = this.olmSessionData
        val olmSession = deserializeFromRealm<OlmSession>(olmSessionStr)!!
        val pickledOlmSession = olmSession.pickle(pickleKey, StringBuffer()).asString()
        return PickledSession(
                pickle = pickledOlmSession,
                senderKey = deviceKey,
                createdUsingFallbackKey = false,
                creationTime = lastReceivedMessageTs.toString(),
                lastUseTime = lastReceivedMessageTs.toString()
        )
    }

    private fun ByteArray.asString() = String(this, charset)
}
