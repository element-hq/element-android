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

package org.matrix.android.sdk.internal.auth.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateAuthTo005(realm: DynamicRealm) : RealmMigrator(realm, 5) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Update SessionParamsEntity to add LoginType")

        realm.schema.get("SessionParamsEntity")
                ?.addField(SessionParamsEntityFields.LOGIN_TYPE, String::class.java)
                ?.setRequired(SessionParamsEntityFields.LOGIN_TYPE, true)
                ?.transform { it.set(SessionParamsEntityFields.LOGIN_TYPE, LoginType.UNKNOWN.name) }
    }
}
