/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

import io.realm.kotlin.where
import okhttp3.internal.toImmutableList
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.internal.crypto.model.InboundGroupSessionData
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmInboundGroupSession
import org.matrix.olm.OlmSession
import org.matrix.rustcomponents.sdk.crypto.CrossSigningKeyExport
import org.matrix.rustcomponents.sdk.crypto.MigrationData
import org.matrix.rustcomponents.sdk.crypto.PickledAccount
import org.matrix.rustcomponents.sdk.crypto.PickledInboundGroupSession
import org.matrix.rustcomponents.sdk.crypto.PickledSession
import timber.log.Timber
import java.nio.charset.Charset

sealed class RealmToMigrate {
    data class DynamicRealm(val realm: io.realm.DynamicRealm) : RealmToMigrate()
    data class ClassicRealm(val realm: io.realm.Realm) : RealmToMigrate()
}

fun RealmToMigrate.hasExistingData(): Boolean {
    return when (this) {
        is RealmToMigrate.ClassicRealm -> {
            !this.realm.isEmpty &&
                    // Check if there is a MetaData object
                    this.realm.where<CryptoMetadataEntity>().count() > 0 &&
                    this.realm.where<CryptoMetadataEntity>().findFirst()?.olmAccountData != null
        }
        is RealmToMigrate.DynamicRealm -> {
            return true
        }
    }
}

@Throws
fun RealmToMigrate.getPickledAccount(pickleKey: ByteArray): MigrationData {
    return when (this) {
        is RealmToMigrate.ClassicRealm -> {
            val metadataEntity = realm.where<CryptoMetadataEntity>().findFirst()
                    ?: throw java.lang.IllegalArgumentException("Rust db migration: No existing metadataEntity")

            val masterKey = metadataEntity.xSignMasterPrivateKey
            val userKey = metadataEntity.xSignUserPrivateKey
            val selfSignedKey = metadataEntity.xSignSelfSignedPrivateKey

            Timber.i("## Migration: has private MSK ${masterKey.isNullOrBlank().not()}")
            Timber.i("## Migration: has private USK ${userKey.isNullOrBlank().not()}")
            Timber.i("## Migration: has private SSK ${selfSignedKey.isNullOrBlank().not()}")

            val userId = metadataEntity.userId
                    ?: throw java.lang.IllegalArgumentException("Rust db migration: userId is null")
            val deviceId = metadataEntity.deviceId
                    ?: throw java.lang.IllegalArgumentException("Rust db migration: deviceID is null")

            val backupVersion = metadataEntity.backupVersion
            val backupRecoveryKey = metadataEntity.keyBackupRecoveryKey

            Timber.i("## Migration: has private backup key ${backupRecoveryKey != null} for version $backupVersion")

            val isOlmAccountShared = metadataEntity.deviceKeysSentToServer

            val olmAccount = metadataEntity.getOlmAccount()
                    ?: throw java.lang.IllegalArgumentException("Rust db migration: No existing account")
            val pickledOlmAccount = olmAccount.pickle(pickleKey, StringBuffer()).asString()

            val pickledAccount = PickledAccount(
                    userId = userId,
                    deviceId = deviceId,
                    pickle = pickledOlmAccount,
                    shared = isOlmAccountShared,
                    uploadedSignedKeyCount = 50
            )
            MigrationData(
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
        }
        is RealmToMigrate.DynamicRealm -> {
            val cryptoMetadataEntitySchema = realm.schema.get("CryptoMetadataEntity")
                    ?: throw java.lang.IllegalStateException("Missing Metadata entity")

            var migrationData: MigrationData? = null
            cryptoMetadataEntitySchema.transform { dynMetaData ->

                val serializedOlmAccount = dynMetaData.getString(CryptoMetadataEntityFields.OLM_ACCOUNT_DATA)

                val masterKey = dynMetaData.getString(CryptoMetadataEntityFields.X_SIGN_MASTER_PRIVATE_KEY)
                val userKey = dynMetaData.getString(CryptoMetadataEntityFields.X_SIGN_USER_PRIVATE_KEY)
                val selfSignedKey = dynMetaData.getString(CryptoMetadataEntityFields.X_SIGN_SELF_SIGNED_PRIVATE_KEY)

                val userId = dynMetaData.getString(CryptoMetadataEntityFields.USER_ID)
                        ?: throw java.lang.IllegalArgumentException("Rust db migration: userId is null")
                val deviceId = dynMetaData.getString(CryptoMetadataEntityFields.DEVICE_ID)
                        ?: throw java.lang.IllegalArgumentException("Rust db migration: deviceID is null")

                val backupVersion = dynMetaData.getString(CryptoMetadataEntityFields.BACKUP_VERSION)
                val backupRecoveryKey = dynMetaData.getString(CryptoMetadataEntityFields.KEY_BACKUP_RECOVERY_KEY)

                val isOlmAccountShared = dynMetaData.getBoolean(CryptoMetadataEntityFields.DEVICE_KEYS_SENT_TO_SERVER)

                val olmAccount = deserializeFromRealm<OlmAccount>(serializedOlmAccount)
                        ?: throw java.lang.IllegalArgumentException("Rust db migration: No existing account")

                val pickledOlmAccount = olmAccount.pickle(pickleKey, StringBuffer()).asString()

                val pickledAccount = PickledAccount(
                        userId = userId,
                        deviceId = deviceId,
                        pickle = pickledOlmAccount,
                        shared = isOlmAccountShared,
                        uploadedSignedKeyCount = 50
                )

                migrationData = MigrationData(
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
            }
            migrationData!!
        }
    }
}

fun RealmToMigrate.trackedUsersChunk(chunkSize: Int, onChunk: ((List<String>) -> Unit)) {
    when (this) {
        is RealmToMigrate.ClassicRealm -> {
            realm.where<UserEntity>()
                    .findAll()
                    .chunked(chunkSize)
                    .onEach {
                        onChunk(it.mapNotNull { it.userId })
                    }
        }
        is RealmToMigrate.DynamicRealm ->  {
            val userList = mutableListOf<String>()
            realm.schema.get("UserEntity")?.transform {
                val userId = it.getString(UserEntityFields.USER_ID)
                // should we check the tracking status?
                userList.add(userId)
                if (userList.size > chunkSize) {
                    onChunk(userList.toImmutableList())
                    userList.clear()
                }
            }
            if (userList.isNotEmpty()) {
                onChunk(userList)
            }
        }
    }
}

fun RealmToMigrate.pickledOlmSessions(pickleKey: ByteArray, chunkSize: Int, onChunk: ((List<PickledSession>) -> Unit)) {
    when (this) {
        is RealmToMigrate.ClassicRealm -> {
            realm.where<OlmSessionEntity>().findAll()
                    .chunked(chunkSize) { chunk ->
                        val export = chunk.map { it.toPickledSession(pickleKey) }
                        onChunk(export)
                    }
        }
        is RealmToMigrate.DynamicRealm ->  {
            val pickledSessions = mutableListOf<PickledSession>()
            realm.schema.get("OlmSessionEntity")?.transform {
                val sessionData = it.getString(OlmSessionEntityFields.OLM_SESSION_DATA)
                val deviceKey = it.getString(OlmSessionEntityFields.DEVICE_KEY)
                val lastReceivedMessageTs = it.getLong(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS)
                val olmSession = deserializeFromRealm<OlmSession>(sessionData)!!
                val pickle = olmSession.pickle(pickleKey, StringBuffer()).asString()
                val pickledSession =  PickledSession(
                        pickle = pickle,
                        senderKey = deviceKey,
                        createdUsingFallbackKey = false,
                        creationTime = lastReceivedMessageTs.toString(),
                        lastUseTime = lastReceivedMessageTs.toString()
                )
                // should we check the tracking status?
                pickledSessions.add(pickledSession)
                if (pickledSessions.size > chunkSize) {
                    onChunk(pickledSessions.toImmutableList())
                    pickledSessions.clear()
                }
            }
            if (pickledSessions.isNotEmpty()) {
                onChunk(pickledSessions)
            }
        }
    }
}

private val sessionDataAdapter = MoshiProvider.providesMoshi()
        .adapter(InboundGroupSessionData::class.java)
fun RealmToMigrate.pickledOlmGroupSessions(pickleKey: ByteArray, chunkSize: Int, onChunk: ((List<PickledInboundGroupSession>) -> Unit)) {
    when (this) {
        is RealmToMigrate.ClassicRealm -> {
            realm.where<OlmInboundGroupSessionEntity>()
                    .findAll()
                    .chunked(chunkSize) { chunk ->
                        val export = chunk.mapNotNull { it.toPickledInboundGroupSession(pickleKey) }
                        onChunk(export)
                    }
        }
        is RealmToMigrate.DynamicRealm ->  {
            val pickledSessions = mutableListOf<PickledInboundGroupSession>()
            realm.schema.get("OlmInboundGroupSessionEntity")?.transform {
                val senderKey = it.getString(OlmInboundGroupSessionEntityFields.SENDER_KEY)
                val roomId = it.getString(OlmInboundGroupSessionEntityFields.ROOM_ID)
                val backedUp = it.getBoolean(OlmInboundGroupSessionEntityFields.BACKED_UP)
                val serializedOlmInboundGroupSession = it.getString(OlmInboundGroupSessionEntityFields.SERIALIZED_OLM_INBOUND_GROUP_SESSION)
                val inboundSession = deserializeFromRealm<OlmInboundGroupSession>(serializedOlmInboundGroupSession) ?: return@transform Unit.also {
                    Timber.w("Rust db migration: Failed to migrated group session, no meta data")
                }
                val sessionData = it.getString(OlmInboundGroupSessionEntityFields.INBOUND_GROUP_SESSION_DATA_JSON).let { json ->
                    sessionDataAdapter.fromJson(json)
                } ?: return@transform Unit.also {
                    Timber.w("Rust db migration: Failed to migrated group session, no meta data")
                }
                val pickle = inboundSession.pickle(pickleKey, StringBuffer()).asString()
                val pickledSession =  PickledInboundGroupSession(
                        pickle = pickle,
                        senderKey = senderKey,
                        signingKey = sessionData.keysClaimed.orEmpty(),
                        roomId = roomId,
                        forwardingChains = sessionData.forwardingCurve25519KeyChain.orEmpty(),
                        imported = sessionData.trusted.orFalse().not(),
                        backedUp = backedUp
                )
                // should we check the tracking status?
                pickledSessions.add(pickledSession)
                if (pickledSessions.size > chunkSize) {
                    onChunk(pickledSessions.toImmutableList())
                    pickledSessions.clear()
                }
            }
            if (pickledSessions.isNotEmpty()) {
                onChunk(pickledSessions)
            }
        }
    }
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

private val charset = Charset.forName("UTF-8")
private fun ByteArray.asString() = String(this, charset)
