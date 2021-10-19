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

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.DynamicRealm
import io.realm.RealmMigration
import timber.log.Timber

internal object RealmIdentityStoreMigration : RealmMigration {

    const val IDENTITY_STORE_SCHEMA_VERSION = 1L

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.v("Migrating Realm Identity from $oldVersion to $newVersion")

        if (oldVersion <= 0) migrateTo1(realm)
    }

    private fun migrateTo1(realm: DynamicRealm) {
        Timber.d("Step 0 -> 1")
        Timber.d("Add field userConsent (Boolean) and set the value to false")

        realm.schema.get("IdentityDataEntity")
                ?.addField(IdentityDataEntityFields.USER_CONSENT, Boolean::class.java)
    }
}
