/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.model.InboundGroupSessionData
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutboundGroupSessionInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

/**
 * Version 17L enhance OlmInboundGroupSessionEntity to support shared history for MSC3061.
 * Also migrates how megolm session are stored to avoid additional serialized frozen class.
 */
internal class MigrateCryptoTo017(realm: DynamicRealm) : RealmMigrator(realm, 17) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("CryptoRoomEntity")
                ?.addField(CryptoRoomEntityFields.SHOULD_SHARE_HISTORY, Boolean::class.java)?.transform {
                    // We don't have access to the session database to check for the state here and set the good value.
                    // But for now as it's behind a lab flag, will set to false and force initial sync when enabled
                    it.setBoolean(CryptoRoomEntityFields.SHOULD_SHARE_HISTORY, false)
                }

        realm.schema.get("OutboundGroupSessionInfoEntity")
                ?.addField(OutboundGroupSessionInfoEntityFields.SHOULD_SHARE_HISTORY, Boolean::class.java)?.transform {
                    // We don't have access to the session database to check for the state here and set the good value.
                    // But for now as it's behind a lab flag, will set to false and force initial sync when enabled
                    it.setBoolean(OutboundGroupSessionInfoEntityFields.SHOULD_SHARE_HISTORY, false)
                }

        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.ENABLE_KEY_FORWARDING_ON_INVITE, Boolean::class.java)
                ?.transform { obj ->
                    // default to false
                    obj.setBoolean(CryptoMetadataEntityFields.ENABLE_KEY_FORWARDING_ON_INVITE, false)
                }

        val moshiAdapter = MoshiProvider.providesMoshi().adapter(InboundGroupSessionData::class.java)

        realm.schema.get("OlmInboundGroupSessionEntity")
                ?.addField(OlmInboundGroupSessionEntityFields.SHARED_HISTORY, Boolean::class.java)
                ?.addField(OlmInboundGroupSessionEntityFields.ROOM_ID, String::class.java)
                ?.addField(OlmInboundGroupSessionEntityFields.INBOUND_GROUP_SESSION_DATA_JSON, String::class.java)
                ?.addField(OlmInboundGroupSessionEntityFields.SERIALIZED_OLM_INBOUND_GROUP_SESSION, String::class.java)
                ?.transform { dynamicObject ->
                    try {
                        // we want to convert the old wrapper frozen class into a
                        // map of sessionData & the pickled session herself
                        dynamicObject.getString(OlmInboundGroupSessionEntityFields.OLM_INBOUND_GROUP_SESSION_DATA)?.let { oldData ->
                            val oldWrapper = tryOrNull("Failed to convert megolm inbound group data") {
                                @Suppress("DEPRECATION")
                                deserializeFromRealm<org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2?>(oldData)
                            }
                            val groupSession = oldWrapper?.olmInboundGroupSession
                                    ?: return@transform Unit.also {
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

                            dynamicObject.setString(OlmInboundGroupSessionEntityFields.INBOUND_GROUP_SESSION_DATA_JSON, moshiAdapter.toJson(data))
                            dynamicObject.setString(OlmInboundGroupSessionEntityFields.SERIALIZED_OLM_INBOUND_GROUP_SESSION, serializeForRealm(groupSession))

                            // denormalized fields
                            dynamicObject.setString(OlmInboundGroupSessionEntityFields.ROOM_ID, oldWrapper.roomId)
                            dynamicObject.setBoolean(OlmInboundGroupSessionEntityFields.SHARED_HISTORY, false)
                        }
                    } catch (failure: Throwable) {
                        Timber.e(failure, "Failed to migrate megolm session")
                    }
                }
    }
}
