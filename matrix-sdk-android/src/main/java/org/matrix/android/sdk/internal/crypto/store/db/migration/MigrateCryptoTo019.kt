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

import io.realm.DynamicRealm
import io.realm.DynamicRealmObject
import org.matrix.android.sdk.api.session.crypto.crosssigning.KeyUsage
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * This migration is adding support for trusted flags on megolm sessions.
 * We can't really assert the trust of existing keys, so for the sake of simplicity we are going to
 * mark existing keys as safe.
 * This migration can take long depending on the account
 */
internal class MigrateCryptoTo019(realm: DynamicRealm) : RealmMigrator(realm, 19) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("CrossSigningInfoEntity")
                ?.addField(CrossSigningInfoEntityFields.WAS_USER_VERIFIED_ONCE, Boolean::class.java)
                ?.transform { dynamicObject ->

                    val knowKeys = dynamicObject.getList(CrossSigningInfoEntityFields.CROSS_SIGNING_KEYS.`$`)
                    val msk = knowKeys.firstOrNull {
                        it.getList(KeyInfoEntityFields.USAGES.`$`, String::class.java).orEmpty().contains(KeyUsage.MASTER.value)
                    }
                    val ssk = knowKeys.firstOrNull {
                        it.getList(KeyInfoEntityFields.USAGES.`$`, String::class.java).orEmpty().contains(KeyUsage.SELF_SIGNING.value)
                    }
                    val isTrusted = isDynamicKeyInfoTrusted(msk?.get<DynamicRealmObject>(KeyInfoEntityFields.TRUST_LEVEL_ENTITY.`$`)) &&
                            isDynamicKeyInfoTrusted(ssk?.get<DynamicRealmObject>(KeyInfoEntityFields.TRUST_LEVEL_ENTITY.`$`))

                    dynamicObject.setBoolean(CrossSigningInfoEntityFields.WAS_USER_VERIFIED_ONCE, isTrusted)
                }
    }

    private fun isDynamicKeyInfoTrusted(keyInfo: DynamicRealmObject?): Boolean {
        if (keyInfo == null) return false
        return !keyInfo.isNull(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED) && keyInfo.getBoolean(TrustLevelEntityFields.CROSS_SIGNED_VERIFIED) &&
                !keyInfo.isNull(TrustLevelEntityFields.LOCALLY_VERIFIED) && keyInfo.getBoolean(TrustLevelEntityFields.LOCALLY_VERIFIED)
    }
}
