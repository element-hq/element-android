/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.store.db

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.store.db.model.CrossSigningInfoEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.CryptoMetadataEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.DeviceInfoEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.GossipingEventEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.IncomingGossipingRequestEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.KeyInfoEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.OutgoingGossipingRequestEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.TrustLevelEntityFields
import im.vector.matrix.android.internal.crypto.store.db.model.UserEntityFields
import im.vector.matrix.android.internal.di.SerializeNulls
import io.realm.DynamicRealm
import io.realm.RealmMigration
import timber.log.Timber

internal object RealmCryptoStoreMigration : RealmMigration {

    // Version 1L added Cross Signing info persistence
    const val CRYPTO_STORE_SCHEMA_VERSION = 2L

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.v("Migrating Realm Crypto from $oldVersion to $newVersion")

        if (oldVersion <= 0) migrateTo1(realm)
        if (oldVersion <= 1) migrateTo2(realm)
    }

    private fun migrateTo1(realm: DynamicRealm) {
        Timber.d("Step 0 -> 1")
        Timber.d("Create KeyInfoEntity")

        val trustLevelentityEntitySchema = realm.schema.create("TrustLevelEntity")
                .addField(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED, Boolean::class.java)
                .setNullable(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED, true)
                .addField(TrustLevelEntityFields.LOCALLY_VERIFIED, Boolean::class.java)
                .setNullable(TrustLevelEntityFields.LOCALLY_VERIFIED, true)

        val keyInfoEntitySchema = realm.schema.create("KeyInfoEntity")
                .addField(KeyInfoEntityFields.PUBLIC_KEY_BASE64, String::class.java)
                .addField(KeyInfoEntityFields.SIGNATURES, String::class.java)
                .addRealmListField(KeyInfoEntityFields.USAGES.`$`, String::class.java)
                .addRealmObjectField(KeyInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevelentityEntitySchema)

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
                ?.addRealmObjectField(DeviceInfoEntityFields.TRUST_LEVEL_ENTITY.`$`, trustLevelentityEntitySchema)
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

    private fun migrateTo2(realm: DynamicRealm) {
        Timber.d("Step 1 -> 2")
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
}
