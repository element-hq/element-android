/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store.db

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.realm.DynamicRealm
import io.realm.RealmMigration
import io.realm.RealmObjectSchema
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.model.MXDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CrossSigningKeysMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.GossipingEventEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.IncomingGossipingRequestEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutboundGroupSessionInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingGossipingRequestEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SerializeNulls
import org.matrix.androidsdk.crypto.data.MXOlmInboundGroupSession2
import timber.log.Timber
import org.matrix.androidsdk.crypto.data.MXDeviceInfo as LegacyMXDeviceInfo

internal object RealmCryptoStoreMigration : RealmMigration {

    // 0, 1, 2: legacy Riot-Android
    // 3: migrate to RiotX schema
    // 4, 5, 6, 7, 8, 9: migrations from RiotX (which was previously 1, 2, 3, 4, 5, 6)
    const val CRYPTO_STORE_SCHEMA_VERSION = 12L

    private fun RealmObjectSchema.addFieldIfNotExists(fieldName: String, fieldType: Class<*>): RealmObjectSchema {
        if (!hasField(fieldName)) {
            addField(fieldName, fieldType)
        }
        return this
    }

    private fun RealmObjectSchema.removeFieldIfExists(fieldName: String): RealmObjectSchema {
        if (hasField(fieldName)) {
            removeField(fieldName)
        }
        return this
    }

    private fun RealmObjectSchema.setRequiredIfNotAlready(fieldName: String, isRequired: Boolean): RealmObjectSchema {
        if (isRequired != isRequired(fieldName)) {
            setRequired(fieldName, isRequired)
        }
        return this
    }

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.v("Migrating Realm Crypto from $oldVersion to $newVersion")

        if (oldVersion <= 0) migrateTo1Legacy(realm)
        if (oldVersion <= 1) migrateTo2Legacy(realm)
        if (oldVersion <= 2) migrateTo3RiotX(realm)
        if (oldVersion <= 3) migrateTo4(realm)
        if (oldVersion <= 4) migrateTo5(realm)
        if (oldVersion <= 5) migrateTo6(realm)
        if (oldVersion <= 6) migrateTo7(realm)
        if (oldVersion <= 7) migrateTo8(realm)
        if (oldVersion <= 8) migrateTo9(realm)
        if (oldVersion <= 9) migrateTo10(realm)
        if (oldVersion <= 10) migrateTo11(realm)
        if (oldVersion <= 11) migrateTo12(realm)
    }

    private fun migrateTo1Legacy(realm: DynamicRealm) {
        Timber.d("Step 0 -> 1")
        Timber.d("Add field lastReceivedMessageTs (Long) and set the value to 0")

        realm.schema.get("OlmSessionEntity")
                ?.addField(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, Long::class.java)
                ?.transform {
                    it.setLong(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, 0)
                }
    }

    private fun migrateTo2Legacy(realm: DynamicRealm) {
        Timber.d("Step 1 -> 2")
        Timber.d("Update IncomingRoomKeyRequestEntity format: requestBodyString field is exploded into several fields")

        realm.schema.get("IncomingRoomKeyRequestEntity")
                ?.addFieldIfNotExists("requestBodyAlgorithm", String::class.java)
                ?.addFieldIfNotExists("requestBodyRoomId", String::class.java)
                ?.addFieldIfNotExists("requestBodySenderKey", String::class.java)
                ?.addFieldIfNotExists("requestBodySessionId", String::class.java)
                ?.transform { dynamicObject ->
                    try {
                        val requestBodyString = dynamicObject.getString("requestBodyString")
                        // It was a map before
                        val map: Map<String, String>? = deserializeFromRealm(requestBodyString)

                        map?.let {
                            dynamicObject.setString("requestBodyAlgorithm", it["algorithm"])
                            dynamicObject.setString("requestBodyRoomId", it["room_id"])
                            dynamicObject.setString("requestBodySenderKey", it["sender_key"])
                            dynamicObject.setString("requestBodySessionId", it["session_id"])
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error")
                    }
                }
                ?.removeFieldIfExists("requestBodyString")

        Timber.d("Update IncomingRoomKeyRequestEntity format: requestBodyString field is exploded into several fields")

        realm.schema.get("OutgoingRoomKeyRequestEntity")
                ?.addFieldIfNotExists("requestBodyAlgorithm", String::class.java)
                ?.addFieldIfNotExists("requestBodyRoomId", String::class.java)
                ?.addFieldIfNotExists("requestBodySenderKey", String::class.java)
                ?.addFieldIfNotExists("requestBodySessionId", String::class.java)
                ?.transform { dynamicObject ->
                    try {
                        val requestBodyString = dynamicObject.getString("requestBodyString")
                        // It was a map before
                        val map: Map<String, String>? = deserializeFromRealm(requestBodyString)

                        map?.let {
                            dynamicObject.setString("requestBodyAlgorithm", it["algorithm"])
                            dynamicObject.setString("requestBodyRoomId", it["room_id"])
                            dynamicObject.setString("requestBodySenderKey", it["sender_key"])
                            dynamicObject.setString("requestBodySessionId", it["session_id"])
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error")
                    }
                }
                ?.removeFieldIfExists("requestBodyString")

        Timber.d("Create KeysBackupDataEntity")

        if (!realm.schema.contains("KeysBackupDataEntity")) {
            realm.schema.create("KeysBackupDataEntity")
                    .addField(KeysBackupDataEntityFields.PRIMARY_KEY, Integer::class.java)
                    .addPrimaryKey(KeysBackupDataEntityFields.PRIMARY_KEY)
                    .setRequired(KeysBackupDataEntityFields.PRIMARY_KEY, true)
                    .addField(KeysBackupDataEntityFields.BACKUP_LAST_SERVER_HASH, String::class.java)
                    .addField(KeysBackupDataEntityFields.BACKUP_LAST_SERVER_NUMBER_OF_KEYS, Integer::class.java)
        }
    }

    private fun migrateTo3RiotX(realm: DynamicRealm) {
        Timber.d("Step 2 -> 3")
        Timber.d("Migrate to RiotX model")

        realm.schema.get("CryptoRoomEntity")
                ?.addFieldIfNotExists(CryptoRoomEntityFields.SHOULD_ENCRYPT_FOR_INVITED_MEMBERS, Boolean::class.java)
                ?.setRequiredIfNotAlready(CryptoRoomEntityFields.SHOULD_ENCRYPT_FOR_INVITED_MEMBERS, false)

        // Convert format of MXDeviceInfo, package has to be the same.
        realm.schema.get("DeviceInfoEntity")
                ?.transform { obj ->
                    try {
                        val oldSerializedData = obj.getString("deviceInfoData")
                        deserializeFromRealm<LegacyMXDeviceInfo>(oldSerializedData)?.let { legacyMxDeviceInfo ->
                            val newMxDeviceInfo = MXDeviceInfo(
                                    deviceId = legacyMxDeviceInfo.deviceId,
                                    userId = legacyMxDeviceInfo.userId,
                                    algorithms = legacyMxDeviceInfo.algorithms,
                                    keys = legacyMxDeviceInfo.keys,
                                    signatures = legacyMxDeviceInfo.signatures,
                                    unsigned = legacyMxDeviceInfo.unsigned,
                                    verified = legacyMxDeviceInfo.mVerified
                            )

                            obj.setString("deviceInfoData", serializeForRealm(newMxDeviceInfo))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error")
                    }
                }

        // Convert MXOlmInboundGroupSession2 to OlmInboundGroupSessionWrapper
        realm.schema.get("OlmInboundGroupSessionEntity")
                ?.transform { obj ->
                    try {
                        val oldSerializedData = obj.getString("olmInboundGroupSessionData")
                        deserializeFromRealm<MXOlmInboundGroupSession2>(oldSerializedData)?.let { mxOlmInboundGroupSession2 ->
                            val sessionKey = mxOlmInboundGroupSession2.mSession.sessionIdentifier()
                            val newOlmInboundGroupSessionWrapper = OlmInboundGroupSessionWrapper(sessionKey, false)
                                    .apply {
                                        olmInboundGroupSession = mxOlmInboundGroupSession2.mSession
                                        roomId = mxOlmInboundGroupSession2.mRoomId
                                        senderKey = mxOlmInboundGroupSession2.mSenderKey
                                        keysClaimed = mxOlmInboundGroupSession2.mKeysClaimed
                                        forwardingCurve25519KeyChain = mxOlmInboundGroupSession2.mForwardingCurve25519KeyChain
                                    }

                            obj.setString("olmInboundGroupSessionData", serializeForRealm(newOlmInboundGroupSessionWrapper))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error")
                    }
                }
    }

    // Version 4L added Cross Signing info persistence
    private fun migrateTo4(realm: DynamicRealm) {
        Timber.d("Step 3 -> 4")

        if (realm.schema.contains("TrustLevelEntity")) {
            Timber.d("Skipping Step 3 -> 4 because entities already exist")
            return
        }

        Timber.d("Create KeyInfoEntity")
        val trustLevelEntityEntitySchema = realm.schema.create("TrustLevelEntity")
                .addField(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED, Boolean::class.java)
                .setNullable(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED, true)
                .addField(TrustLevelEntityFields.LOCALLY_VERIFIED, Boolean::class.java)
                .setNullable(TrustLevelEntityFields.LOCALLY_VERIFIED, true)

        val keyInfoEntitySchema = realm.schema.create("KeyInfoEntity")
                .addField(KeyInfoEntityFields.PUBLIC_KEY_BASE64, String::class.java)
                .addField(KeyInfoEntityFields.SIGNATURES, String::class.java)
                .addRealmListField(KeyInfoEntityFields.USAGES.`$`, String::class.java)
                .addRealmObjectField(KeyInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevelEntityEntitySchema)

        Timber.d("Create CrossSigningInfoEntity")

        val crossSigningInfoSchema = realm.schema.create("CrossSigningInfoEntity")
                .addField(CrossSigningInfoEntityFields.USER_ID, String::class.java)
                .addPrimaryKey(CrossSigningInfoEntityFields.USER_ID)
                .addRealmListField(CrossSigningInfoEntityFields.CROSS_SIGNING_KEYS.`$`, keyInfoEntitySchema)

        Timber.d("Updating UserEntity table")
        realm.schema.get("UserEntity")
                ?.addRealmObjectField(UserEntityFields.CROSS_SIGNING_INFO_ENTITY.`$`, crossSigningInfoSchema)

        Timber.d("Updating CryptoMetadataEntity table")
        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.X_SIGN_MASTER_PRIVATE_KEY, String::class.java)
                ?.addField(CryptoMetadataEntityFields.X_SIGN_USER_PRIVATE_KEY, String::class.java)
                ?.addField(CryptoMetadataEntityFields.X_SIGN_SELF_SIGNED_PRIVATE_KEY, String::class.java)

        val moshi = Moshi.Builder().add(SerializeNulls.JSON_ADAPTER_FACTORY).build()
        val listMigrationAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(
                List::class.java,
                String::class.java,
                Any::class.java
        ))
        val mapMigrationAdapter = moshi.adapter<JsonDict>(Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
        ))

        realm.schema.get("DeviceInfoEntity")
                ?.addField(DeviceInfoEntityFields.USER_ID, String::class.java)
                ?.addField(DeviceInfoEntityFields.ALGORITHM_LIST_JSON, String::class.java)
                ?.addField(DeviceInfoEntityFields.KEYS_MAP_JSON, String::class.java)
                ?.addField(DeviceInfoEntityFields.SIGNATURE_MAP_JSON, String::class.java)
                ?.addField(DeviceInfoEntityFields.UNSIGNED_MAP_JSON, String::class.java)
                ?.addField(DeviceInfoEntityFields.IS_BLOCKED, Boolean::class.java)
                ?.setNullable(DeviceInfoEntityFields.IS_BLOCKED, true)
                ?.addRealmObjectField(DeviceInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevelEntityEntitySchema)
                ?.transform { obj ->

                    try {
                        val oldSerializedData = obj.getString("deviceInfoData")
                        deserializeFromRealm<MXDeviceInfo>(oldSerializedData)?.let { oldDevice ->

                            val trustLevel = realm.createObject("TrustLevelEntity")
                            when (oldDevice.verified) {
                                MXDeviceInfo.DEVICE_VERIFICATION_UNKNOWN    -> {
                                    obj.setNull(DeviceInfoEntityFields.TRUST_LEVEL_ENTITY.`$`)
                                }
                                MXDeviceInfo.DEVICE_VERIFICATION_BLOCKED    -> {
                                    trustLevel.setNull(TrustLevelEntityFields.LOCALLY_VERIFIED)
                                    trustLevel.setNull(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED)
                                    obj.setBoolean(DeviceInfoEntityFields.IS_BLOCKED, oldDevice.isBlocked)
                                    obj.setObject(DeviceInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevel)
                                }
                                MXDeviceInfo.DEVICE_VERIFICATION_UNVERIFIED -> {
                                    trustLevel.setBoolean(TrustLevelEntityFields.LOCALLY_VERIFIED, false)
                                    trustLevel.setBoolean(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED, false)
                                    obj.setObject(DeviceInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevel)
                                }
                                MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED   -> {
                                    trustLevel.setBoolean(TrustLevelEntityFields.LOCALLY_VERIFIED, true)
                                    trustLevel.setBoolean(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED, false)
                                    obj.setObject(DeviceInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevel)
                                }
                            }

                            obj.setString(DeviceInfoEntityFields.USER_ID, oldDevice.userId)
                            obj.setString(DeviceInfoEntityFields.IDENTITY_KEY, oldDevice.identityKey())
                            obj.setString(DeviceInfoEntityFields.ALGORITHM_LIST_JSON, listMigrationAdapter.toJson(oldDevice.algorithms))
                            obj.setString(DeviceInfoEntityFields.KEYS_MAP_JSON, mapMigrationAdapter.toJson(oldDevice.keys))
                            obj.setString(DeviceInfoEntityFields.SIGNATURE_MAP_JSON, mapMigrationAdapter.toJson(oldDevice.signatures))
                            obj.setString(DeviceInfoEntityFields.UNSIGNED_MAP_JSON, mapMigrationAdapter.toJson(oldDevice.unsigned))
                        }
                    } catch (failure: Throwable) {
                        Timber.w(failure, "Crypto Data base migration error")
                        // an unfortunate refactor did modify that class, making deserialization failing
                        // so we just skip and ignore..
                    }
                }
                ?.removeField("deviceInfoData")
    }

    private fun migrateTo5(realm: DynamicRealm) {
        Timber.d("Step 4 -> 5")
        realm.schema.remove("OutgoingRoomKeyRequestEntity")
        realm.schema.remove("IncomingRoomKeyRequestEntity")

        // Not need to migrate existing request, just start fresh?

        realm.schema.create("GossipingEventEntity")
                .addField(GossipingEventEntityFields.TYPE, String::class.java)
                .addIndex(GossipingEventEntityFields.TYPE)
                .addField(GossipingEventEntityFields.CONTENT, String::class.java)
                .addField(GossipingEventEntityFields.SENDER, String::class.java)
                .addIndex(GossipingEventEntityFields.SENDER)
                .addField(GossipingEventEntityFields.DECRYPTION_RESULT_JSON, String::class.java)
                .addField(GossipingEventEntityFields.DECRYPTION_ERROR_CODE, String::class.java)
                .addField(GossipingEventEntityFields.AGE_LOCAL_TS, Long::class.java)
                .setNullable(GossipingEventEntityFields.AGE_LOCAL_TS, true)
                .addField(GossipingEventEntityFields.SEND_STATE_STR, String::class.java)

        realm.schema.create("IncomingGossipingRequestEntity")
                .addField(IncomingGossipingRequestEntityFields.REQUEST_ID, String::class.java)
                .addIndex(IncomingGossipingRequestEntityFields.REQUEST_ID)
                .addField(IncomingGossipingRequestEntityFields.TYPE_STR, String::class.java)
                .addIndex(IncomingGossipingRequestEntityFields.TYPE_STR)
                .addField(IncomingGossipingRequestEntityFields.OTHER_USER_ID, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.REQUESTED_INFO_STR, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.OTHER_DEVICE_ID, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.REQUEST_STATE_STR, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.LOCAL_CREATION_TIMESTAMP, Long::class.java)
                .setNullable(IncomingGossipingRequestEntityFields.LOCAL_CREATION_TIMESTAMP, true)

        realm.schema.create("OutgoingGossipingRequestEntity")
                .addField(OutgoingGossipingRequestEntityFields.REQUEST_ID, String::class.java)
                .addIndex(OutgoingGossipingRequestEntityFields.REQUEST_ID)
                .addField(OutgoingGossipingRequestEntityFields.RECIPIENTS_DATA, String::class.java)
                .addField(OutgoingGossipingRequestEntityFields.REQUESTED_INFO_STR, String::class.java)
                .addField(OutgoingGossipingRequestEntityFields.TYPE_STR, String::class.java)
                .addIndex(OutgoingGossipingRequestEntityFields.TYPE_STR)
                .addField(OutgoingGossipingRequestEntityFields.REQUEST_STATE_STR, String::class.java)
    }

    private fun migrateTo6(realm: DynamicRealm) {
        Timber.d("Step 5 -> 6")
        Timber.d("Updating CryptoMetadataEntity table")
        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.KEY_BACKUP_RECOVERY_KEY, String::class.java)
                ?.addField(CryptoMetadataEntityFields.KEY_BACKUP_RECOVERY_KEY_VERSION, String::class.java)
    }

    private fun migrateTo7(realm: DynamicRealm) {
        Timber.d("Step 6 -> 7")
        Timber.d("Updating KeyInfoEntity table")
        val crossSigningKeysMapper = CrossSigningKeysMapper(MoshiProvider.providesMoshi())

        val keyInfoEntities = realm.where("KeyInfoEntity").findAll()
        try {
            keyInfoEntities.forEach {
                val stringSignatures = it.getString(KeyInfoEntityFields.SIGNATURES)
                val objectSignatures: Map<String, Map<String, String>>? = deserializeFromRealm(stringSignatures)
                val jsonSignatures = crossSigningKeysMapper.serializeSignatures(objectSignatures)
                it.setString(KeyInfoEntityFields.SIGNATURES, jsonSignatures)
            }
        } catch (failure: Throwable) {
        }

        // Migrate frozen classes
        val inboundGroupSessions = realm.where("OlmInboundGroupSessionEntity").findAll()
        inboundGroupSessions.forEach { dynamicObject ->
            dynamicObject.getString(OlmInboundGroupSessionEntityFields.OLM_INBOUND_GROUP_SESSION_DATA)?.let { serializedObject ->
                try {
                    deserializeFromRealm<OlmInboundGroupSessionWrapper?>(serializedObject)?.let { oldFormat ->
                        val newFormat = oldFormat.exportKeys()?.let {
                            OlmInboundGroupSessionWrapper2(it)
                        }
                        dynamicObject.setString(OlmInboundGroupSessionEntityFields.OLM_INBOUND_GROUP_SESSION_DATA, serializeForRealm(newFormat))
                    }
                } catch (failure: Throwable) {
                    Timber.e(failure, "## OlmInboundGroupSessionEntity migration failed")
                }
            }
        }
    }

    private fun migrateTo8(realm: DynamicRealm) {
        Timber.d("Step 7 -> 8")
        realm.schema.create("MyDeviceLastSeenInfoEntity")
                .addField(MyDeviceLastSeenInfoEntityFields.DEVICE_ID, String::class.java)
                .addPrimaryKey(MyDeviceLastSeenInfoEntityFields.DEVICE_ID)
                .addField(MyDeviceLastSeenInfoEntityFields.DISPLAY_NAME, String::class.java)
                .addField(MyDeviceLastSeenInfoEntityFields.LAST_SEEN_IP, String::class.java)
                .addField(MyDeviceLastSeenInfoEntityFields.LAST_SEEN_TS, Long::class.java)
                .setNullable(MyDeviceLastSeenInfoEntityFields.LAST_SEEN_TS, true)

        val now = System.currentTimeMillis()
        realm.schema.get("DeviceInfoEntity")
                ?.addField(DeviceInfoEntityFields.FIRST_TIME_SEEN_LOCAL_TS, Long::class.java)
                ?.setNullable(DeviceInfoEntityFields.FIRST_TIME_SEEN_LOCAL_TS, true)
                ?.transform { deviceInfoEntity ->
                    tryOrNull {
                        deviceInfoEntity.setLong(DeviceInfoEntityFields.FIRST_TIME_SEEN_LOCAL_TS, now)
                    }
                }
    }

    // Fixes duplicate devices in UserEntity#devices
    private fun migrateTo9(realm: DynamicRealm) {
        Timber.d("Step 8 -> 9")
        val userEntities = realm.where("UserEntity").findAll()
        userEntities.forEach {
            try {
                val deviceList = it.getList(UserEntityFields.DEVICES.`$`)
                        ?: return@forEach
                val distinct = deviceList.distinctBy { it.getString(DeviceInfoEntityFields.DEVICE_ID) }
                if (distinct.size != deviceList.size) {
                    deviceList.clear()
                    deviceList.addAll(distinct)
                }
            } catch (failure: Throwable) {
                Timber.w(failure, "Crypto Data base migration error for migrateTo9")
            }
        }
    }

    // Version 10L added WithHeld Keys Info (MSC2399)
    private fun migrateTo10(realm: DynamicRealm) {
        Timber.d("Step 9 -> 10")
        realm.schema.create("WithHeldSessionEntity")
                .addField(WithHeldSessionEntityFields.ROOM_ID, String::class.java)
                .addField(WithHeldSessionEntityFields.ALGORITHM, String::class.java)
                .addField(WithHeldSessionEntityFields.SESSION_ID, String::class.java)
                .addIndex(WithHeldSessionEntityFields.SESSION_ID)
                .addField(WithHeldSessionEntityFields.SENDER_KEY, String::class.java)
                .addIndex(WithHeldSessionEntityFields.SENDER_KEY)
                .addField(WithHeldSessionEntityFields.CODE_STRING, String::class.java)
                .addField(WithHeldSessionEntityFields.REASON, String::class.java)

        realm.schema.create("SharedSessionEntity")
                .addField(SharedSessionEntityFields.ROOM_ID, String::class.java)
                .addField(SharedSessionEntityFields.ALGORITHM, String::class.java)
                .addField(SharedSessionEntityFields.SESSION_ID, String::class.java)
                .addIndex(SharedSessionEntityFields.SESSION_ID)
                .addField(SharedSessionEntityFields.USER_ID, String::class.java)
                .addIndex(SharedSessionEntityFields.USER_ID)
                .addField(SharedSessionEntityFields.DEVICE_ID, String::class.java)
                .addIndex(SharedSessionEntityFields.DEVICE_ID)
                .addField(SharedSessionEntityFields.CHAIN_INDEX, Long::class.java)
                .setNullable(SharedSessionEntityFields.CHAIN_INDEX, true)
    }

    // Version 11L added deviceKeysSentToServer boolean to CryptoMetadataEntity
    private fun migrateTo11(realm: DynamicRealm) {
        Timber.d("Step 10 -> 11")
        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.DEVICE_KEYS_SENT_TO_SERVER, Boolean::class.java)
    }

    // Version 12L added outbound group session persistence
    private fun migrateTo12(realm: DynamicRealm) {
        Timber.d("Step 11 -> 12")
        val outboundEntitySchema = realm.schema.create("OutboundGroupSessionInfoEntity")
                .addField(OutboundGroupSessionInfoEntityFields.SERIALIZED_OUTBOUND_SESSION_DATA, String::class.java)
                .addField(OutboundGroupSessionInfoEntityFields.CREATION_TIME, Long::class.java)
                .setNullable(OutboundGroupSessionInfoEntityFields.CREATION_TIME, true)

        realm.schema.get("CryptoRoomEntity")
                ?.addRealmObjectField(CryptoRoomEntityFields.OUTBOUND_SESSION_INFO.`$`, outboundEntitySchema)
    }
}
