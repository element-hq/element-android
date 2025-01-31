/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import org.matrix.androidsdk.crypto.data.MXDeviceInfo
import org.matrix.androidsdk.crypto.data.MXOlmInboundGroupSession2
import timber.log.Timber

internal class MigrateCryptoTo003RiotX(realm: DynamicRealm) : RealmMigrator(realm, 3) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Migrate to RiotX model")
        realm.schema.get("CryptoRoomEntity")
                ?.addFieldIfNotExists(CryptoRoomEntityFields.SHOULD_ENCRYPT_FOR_INVITED_MEMBERS, Boolean::class.java)
                ?.setRequiredIfNotAlready(CryptoRoomEntityFields.SHOULD_ENCRYPT_FOR_INVITED_MEMBERS, false)

        // Convert format of MXDeviceInfo, package has to be the same.
        realm.schema.get("DeviceInfoEntity")
                ?.transform { obj ->
                    try {
                        val oldSerializedData = obj.getString("deviceInfoData")
                        deserializeFromRealm<MXDeviceInfo>(oldSerializedData)?.let { legacyMxDeviceInfo ->
                            val newMxDeviceInfo = org.matrix.android.sdk.api.session.crypto.model.MXDeviceInfo(
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
}
