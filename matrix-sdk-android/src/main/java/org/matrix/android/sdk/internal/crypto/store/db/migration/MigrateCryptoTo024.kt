/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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
import org.matrix.android.sdk.internal.extensions.safeRemove
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateCryptoTo024(realm: DynamicRealm) : RealmMigrator(realm, 24) {
    /**
     * Delete the whole DB, except tables that are still used to store data.
     * Keep:
     * - CryptoMetadataEntity
     * - MyDeviceLastSeenInfoEntity
     * - CryptoRoomEntity (but remove unused member 'outboundSessionInfo: OutboundGroupSessionInfoEntity')
     */
    override fun doMigrate(realm: DynamicRealm) {
        with(realm.schema) {
            get("CryptoRoomEntity")?.removeField("outboundSessionInfo")

            // Warning: order is important, first remove classes that depends on others.
            safeRemove("UserEntity")
            safeRemove("DeviceInfoEntity")
            safeRemove("CrossSigningInfoEntity")
            safeRemove("KeyInfoEntity")
            safeRemove("TrustLevelEntity")
            safeRemove("KeysBackupDataEntity")
            safeRemove("OlmInboundGroupSessionEntity")
            safeRemove("OlmSessionEntity")
            safeRemove("AuditTrailEntity")
            safeRemove("OutgoingKeyRequestEntity")
            safeRemove("KeyRequestReplyEntity")
            safeRemove("WithHeldSessionEntity")
            safeRemove("SharedSessionEntity")
            safeRemove("OutboundGroupSessionInfoEntity")
        }
    }
}
