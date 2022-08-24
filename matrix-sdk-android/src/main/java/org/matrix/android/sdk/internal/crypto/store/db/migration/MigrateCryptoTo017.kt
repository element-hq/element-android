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

import io.realm.kotlin.migration.AutomaticSchemaMigration
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.model.InboundGroupSessionData
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator
import org.matrix.android.sdk.internal.database.safeEnumerate
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

/**
 * Version 17L enhance OlmInboundGroupSessionEntity to support shared history for MSC3061.
 * Also migrates how megolm session are stored to avoid additional serialized frozen class.
 */
internal class MigrateCryptoTo017(context: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(context, 17) {

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        Timber.d("Update CryptoRoomEntity")
        Timber.d("Update OutboundGroupSessionInfoEntity")
        Timber.d("Update CryptoMetadataEntity")
        Timber.d("Update OlmInboundGroupSessionEntity")
        val moshiAdapter = MoshiProvider.providesMoshi().adapter(InboundGroupSessionData::class.java)
        migrationContext.safeEnumerate("OlmInboundGroupSessionEntity") { oldObject, newObject ->
            if (newObject == null) return@safeEnumerate
            try {
                // we want to convert the old wrapper frozen class into a
                // map of sessionData & the pickled session herself
                oldObject.getNullableValue("olmInboundGroupSessionData", String::class)?.let { oldData ->
                    val oldWrapper = tryOrNull("Failed to convert megolm inbound group data") {
                        @Suppress("DEPRECATION")
                        deserializeFromRealm<org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2?>(oldData)
                    }
                    val groupSession = oldWrapper?.olmInboundGroupSession
                            ?: return@safeEnumerate Unit.also {
                                Timber.w("Failed to migrate megolm session, no olmInboundGroupSession")
                            }
                    // now convert to new data
                    val data = InboundGroupSessionData(
                            senderKey = oldWrapper.senderKey,
                            roomId = oldWrapper.roomId,
                            keysClaimed = oldWrapper.keysClaimed,
                            forwardingCurve25519KeyChain = oldWrapper.forwardingCurve25519KeyChain,
                            sharedHistory = false,
                    )

                    newObject.set("inboundGroupSessionDataJson", moshiAdapter.toJson(data))
                    newObject.set("serializedOlmInboundGroupSession", serializeForRealm(groupSession))

                    // denormalized fields
                    newObject.set("roomId", oldWrapper.roomId)
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to migrate megolm session")
            }
        }
    }
}
