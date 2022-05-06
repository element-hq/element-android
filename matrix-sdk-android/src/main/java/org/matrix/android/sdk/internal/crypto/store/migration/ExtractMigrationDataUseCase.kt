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

package org.matrix.android.sdk.internal.crypto.store

import io.realm.Realm
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.olm.OlmUtility
import uniffi.olm.CrossSigningKeyExport
import uniffi.olm.MigrationData
import uniffi.olm.PickledAccount
import uniffi.olm.PickledInboundGroupSession
import uniffi.olm.PickledSession
import java.nio.charset.Charset
import javax.inject.Inject

private val charset = Charset.forName("UTF-8")

internal class ExtractMigrationDataUseCase @Inject constructor() {

    operator fun invoke(realm: Realm): MigrationData? {
        val metadataEntity = realm.where<CryptoMetadataEntity>().findFirst() ?: return null

        val pickleKey = OlmUtility.getRandomKey()
        val olmSessionEntities = realm.where<OlmSessionEntity>().findAll()
        val pickledSessions = olmSessionEntities.map { it.toPickledSession(pickleKey) }

        val inboundGroupSessionEntities = realm.where<OlmInboundGroupSessionEntity>().findAll()
        val pickledInboundGroupSessions = inboundGroupSessionEntities.map { it.toPickledInboundGroupSession(pickleKey) }

        val masterKey = metadataEntity.xSignMasterPrivateKey
        val userKey = metadataEntity.xSignUserPrivateKey
        val selfSignedKey = metadataEntity.xSignSelfSignedPrivateKey

        val userId = metadataEntity.userId!!
        val deviceId = metadataEntity.deviceId!!
        val backupVersion = metadataEntity.backupVersion
        val backupRecoveryKey = metadataEntity.keyBackupRecoveryKey

        val trackedUserEntities = realm.where<UserEntity>().findAll()
        val trackedUserIds = trackedUserEntities.mapNotNull {
            it.userId
        }
        val isOlmAccountShared = metadataEntity.deviceKeysSentToServer

        val olmAccount = metadataEntity.getOlmAccount()!!
        val pickledOlmAccount = olmAccount.pickle(pickleKey)
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
                pickleKey = String(pickleKey, charset),
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

    private fun OlmInboundGroupSessionEntity.toPickledInboundGroupSession(pickleKey: ByteArray): PickledInboundGroupSession {
        val senderKey = this.senderKey ?: ""
        val olmInboundGroupSession = getInboundGroupSession()!!
        val pickledInboundGroupSession = olmInboundGroupSession.olmInboundGroupSession!!.pickle(pickleKey)
        return PickledInboundGroupSession(
                pickle = pickledInboundGroupSession,
                senderKey = senderKey,
                signingKey = olmInboundGroupSession.keysClaimed.orEmpty(),
                roomId = olmInboundGroupSession.roomId!!,
                forwardingChains = olmInboundGroupSession.forwardingCurve25519KeyChain.orEmpty(),
                imported = true,
                backedUp = backedUp
        )
    }

    private fun OlmSessionEntity.toPickledSession(pickleKey: ByteArray): PickledSession {
        val deviceKey = this.deviceKey ?: ""
        val lastReceivedMessageTs = this.lastReceivedMessageTs
        val olmSession = getOlmSession()!!
        val pickledOlmSession = olmSession.pickle(pickleKey)
        return PickledSession(
                pickle = pickledOlmSession,
                senderKey = deviceKey,
                createdUsingFallbackKey = false,
                creationTime = lastReceivedMessageTs.toString(),
                lastUseTime = lastReceivedMessageTs.toString()
        )
    }

    private fun Any.pickle(pickleKey: ByteArray): String {
        return try {
            val pickleMethod = this.javaClass.getDeclaredMethod("serialize", ByteArray::class.java, StringBuffer::class.java)
            pickleMethod.isAccessible = true
            val pickled = pickleMethod.invoke(this, pickleKey, StringBuffer())!!
            String(pickled as ByteArray, charset)
        } catch (throwable: Throwable) {
            ""
        }
    }
}
