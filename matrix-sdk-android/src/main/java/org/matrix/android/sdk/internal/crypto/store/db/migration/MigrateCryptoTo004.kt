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

package org.matrix.android.sdk.internal.crypto.store.db.migration

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.realm.DynamicRealm
import org.matrix.android.sdk.api.session.crypto.model.MXDeviceInfo
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.di.SerializeNulls
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

// Version 4L added Cross Signing info persistence
internal class MigrateCryptoTo004(realm: DynamicRealm) : RealmMigrator(realm, 4) {

    override fun doMigrate(realm: DynamicRealm) {
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
}
