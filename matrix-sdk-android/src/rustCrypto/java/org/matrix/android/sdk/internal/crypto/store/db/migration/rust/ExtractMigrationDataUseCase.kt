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

private val charset = Charset.forName("UTF-8")

internal class ExtractMigrationDataUseCase {

    fun extractData(realm: Realm): MigrationData {
        return try {
            extract(realm) ?: throw ExtractMigrationDataFailure
        } catch (failure: Throwable) {
            throw ExtractMigrationDataFailure
        }
    }

    fun hasExistingData(realmConfiguration: RealmConfiguration): Boolean {
        return Realm.getInstance(realmConfiguration).use { realm ->
            !realm.isEmpty &&
                    // Check if there is a MetaData object
                    realm.where<CryptoMetadataEntity>().count() > 0
        }
    }

    private fun extract(realm: Realm): MigrationData? {
        val metadataEntity = realm.where<CryptoMetadataEntity>().findFirst() ?: return null.also {
            Timber.w("Rust db migration: No existing metadataEntity")
        }

        val pickleKey = OlmUtility.getRandomKey()
        val olmSessionEntities = realm.where<OlmSessionEntity>().findAll()
        val pickledSessions = olmSessionEntities.map { it.toPickledSession(pickleKey) }

        val inboundGroupSessionEntities = realm.where<OlmInboundGroupSessionEntity>().findAll()
        val pickledInboundGroupSessions = inboundGroupSessionEntities.mapNotNull { it.toPickledInboundGroupSession(pickleKey) }

        val masterKey = metadataEntity.xSignMasterPrivateKey
        val userKey = metadataEntity.xSignUserPrivateKey
        val selfSignedKey = metadataEntity.xSignSelfSignedPrivateKey

        val userId = metadataEntity.userId ?: return null
        val deviceId = metadataEntity.deviceId ?: return null
        val backupVersion = metadataEntity.backupVersion
        val backupRecoveryKey = metadataEntity.keyBackupRecoveryKey

        val trackedUserEntities = realm.where<UserEntity>().findAll()
        val trackedUserIds = trackedUserEntities.mapNotNull {
            it.userId
        }
        val isOlmAccountShared = metadataEntity.deviceKeysSentToServer

        val olmAccount = metadataEntity.getOlmAccount() ?: return null
        val pickledOlmAccount = olmAccount.pickle(pickleKey, StringBuffer()).asString()
        val pickledAccount = PickledAccount(
                userId = userId,
                deviceId = deviceId,
                pickle = pickledOlmAccount,
                shared = isOlmAccountShared,
                uploadedSignedKeyCount = 50
        )
        return MigrationData(
                account = pickledAccount,
                sessions = pickledSessions,
                inboundGroupSessions = pickledInboundGroupSessions,
                pickleKey = pickleKey.map { it.toUByte() },
                backupVersion = backupVersion,
                backupRecoveryKey = backupRecoveryKey,
                crossSigning = CrossSigningKeyExport(
                        masterKey = masterKey,
                        selfSigningKey = selfSignedKey,
                        userSigningKey = userKey
                ),
                trackedUsers = trackedUserIds
        )
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
                imported = true,
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
