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

import io.realm.kotlin.dynamic.getNullableValue
import io.realm.kotlin.migration.AutomaticSchemaMigration
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CrossSigningKeysMapper
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

internal class MigrateCryptoTo007(context: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(context, 7) {

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        Timber.d("Updating KeyInfoEntity table")
        val crossSigningKeysMapper = CrossSigningKeysMapper(MoshiProvider.providesMoshi())
        val keyInfoEntities = migrationContext.newRealm.query("KeyInfoEntity").find()
        try {
            keyInfoEntities.forEach {
                val stringSignatures: String? = it.getNullableValue("signatures")
                val objectSignatures: Map<String, Map<String, String>>? = deserializeFromRealm(stringSignatures)
                val jsonSignatures = crossSigningKeysMapper.serializeSignatures(objectSignatures)
                it.set("signatures", jsonSignatures)
            }
        } catch (ignore: Throwable) {
        }

        // Migrate frozen classes
        val inboundGroupSessions = migrationContext.newRealm.query("OlmInboundGroupSessionEntity").find()
        inboundGroupSessions.forEach { dynamicObject ->
            dynamicObject.getNullableValue("olmInboundGroupSessionData", String::class)?.let { serializedObject ->
                try {
                    deserializeFromRealm<OlmInboundGroupSessionWrapper?>(serializedObject)?.let { oldFormat ->
                        val newFormat = oldFormat.exportKeys()?.let {
                            OlmInboundGroupSessionWrapper2(it)
                        }
                        dynamicObject.set("olmInboundGroupSessionData", serializeForRealm(newFormat))
                    }
                } catch (failure: Throwable) {
                    Timber.e(failure, "## OlmInboundGroupSessionEntity migration failed")
                }
            }
        }
    }
}
