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

package org.matrix.android.sdk.internal.crypto.store.db.migration.rust

import io.realm.DynamicRealm
import io.realm.DynamicRealmObject
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmSession
import org.matrix.olm.OlmUtility
import uniffi.olm.CrossSigningKeyExport
import uniffi.olm.MigrationData
import uniffi.olm.PickledAccount
import uniffi.olm.PickledInboundGroupSession
import uniffi.olm.PickledSession
import java.nio.charset.Charset

private val charset = Charset.forName("UTF-8")

internal class ExtractMigrationDataUseCase {

    operator fun invoke(realm: DynamicRealm): MigrationData {
        return try {
            extract(realm) ?: throw ExtractMigrationDataFailure
        } catch (failure: Throwable) {
            throw ExtractMigrationDataFailure
        }
    }

    private fun extract(realm: DynamicRealm): MigrationData? {
        val metadataEntity = realm.where("CryptoMetadataEntity").findFirst() ?: return null

        val pickleKey = OlmUtility.getRandomKey()
        val olmSessionEntities = realm.where("OlmSessionEntity").findAll()
        val pickledSessions = olmSessionEntities.map { it.toPickledSession(pickleKey) }

        val inboundGroupSessionEntities = realm.where("OlmInboundGroupSessionEntity").findAll()
        val pickledInboundGroupSessions = inboundGroupSessionEntities.map { it.toPickledInboundGroupSession(pickleKey) }

        val masterKey = metadataEntity.getString("xSignMasterPrivateKey")
        val userKey = metadataEntity.getString("xSignUserPrivateKey")
        val selfSignedKey = metadataEntity.getString("xSignSelfSignedPrivateKey")

        val userId = metadataEntity.getString("userId")!!
        val deviceId = metadataEntity.getString("deviceId")!!
        val backupVersion = metadataEntity.getString("backupVersion")
        val backupRecoveryKey = metadataEntity.getString("keyBackupRecoveryKey")

        val trackedUserEntities = realm.where("UserEntity").findAll()
        val trackedUserIds = trackedUserEntities.mapNotNull {
            it.getString("userId")
        }
        val isOlmAccountShared = metadataEntity.getBoolean("deviceKeysSentToServer")

        val olmAccountStr = metadataEntity.getString("olmAccountData")
        val olmAccount = deserializeFromRealm<OlmAccount>(olmAccountStr)!!
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

    private fun DynamicRealmObject.toPickledInboundGroupSession(pickleKey: ByteArray): PickledInboundGroupSession {
        val senderKey = this.getString("senderKey") ?: ""
        val backedUp = this.getBoolean("backedUp")
        val olmInboundGroupSessionStr = this.getString("olmInboundGroupSessionData")
        val olmInboundGroupSession = deserializeFromRealm<OlmInboundGroupSessionWrapper2>(olmInboundGroupSessionStr)!!
        val pickledInboundGroupSession = olmInboundGroupSession.olmInboundGroupSession!!.pickle(pickleKey, StringBuffer()).asString()
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

    private fun DynamicRealmObject.toPickledSession(pickleKey: ByteArray): PickledSession {
        val deviceKey = this.getString("deviceKey") ?: ""
        val lastReceivedMessageTs = this.getLong("lastReceivedMessageTs")
        val olmSessionStr = this.getString("olmSessionData")
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
