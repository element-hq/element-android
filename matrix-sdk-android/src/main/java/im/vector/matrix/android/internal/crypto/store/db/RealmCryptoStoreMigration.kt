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

import im.vector.matrix.android.internal.crypto.store.db.model.*
import im.vector.matrix.android.internal.crypto.store.db.model.KeyInfoEntity
import io.realm.DynamicRealm
import io.realm.RealmMigration
import timber.log.Timber

internal object RealmCryptoStoreMigration : RealmMigration {

    // Version 1L added Cross Signing info persistence
    const val CRYPTO_STORE_SCHEMA_VERSION = 1L

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.v("Migrating Realm Crypto from $oldVersion to $newVersion")

        if (oldVersion <= 0) {
            Timber.d("Step 0 -> 1")
            Timber.d("Create KeyInfoEntity")

            val keyInfoEntitySchema = realm.schema.create("KeyInfoEntity")
                    .addField(KeyInfoEntityFields.PUBLIC_KEY_BASE64, String::class.java)
                    .addField(KeyInfoEntityFields.SIGNATURES, String::class.java)
                    .addRealmListField(KeyInfoEntityFields.USAGES.`$`, String::class.java)


            Timber.d("Create CrossSigningInfoEntity")

            val crossSigningInfoSchema = realm.schema.create("CrossSigningInfoEntity")
                    .addField(CrossSigningInfoEntityFields.USER_ID, String::class.java)
                    .addField(CrossSigningInfoEntityFields.IS_TRUSTED, Boolean::class.java)
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

        }
    }
}
