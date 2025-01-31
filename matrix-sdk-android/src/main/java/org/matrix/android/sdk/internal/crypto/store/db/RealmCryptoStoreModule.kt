/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db

import io.realm.annotations.RealmModule
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyRequestReplyEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutboundGroupSessionInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntity

/**
 * Realm module for Crypto store classes.
 */
@RealmModule(
        library = true,
        classes = [
            CryptoMetadataEntity::class,
            CryptoRoomEntity::class,
            DeviceInfoEntity::class,
            KeysBackupDataEntity::class,
            OlmInboundGroupSessionEntity::class,
            OlmSessionEntity::class,
            UserEntity::class,
            KeyInfoEntity::class,
            CrossSigningInfoEntity::class,
            TrustLevelEntity::class,
            AuditTrailEntity::class,
            OutgoingKeyRequestEntity::class,
            KeyRequestReplyEntity::class,
            MyDeviceLastSeenInfoEntity::class,
            WithHeldSessionEntity::class,
            SharedSessionEntity::class,
            OutboundGroupSessionInfoEntity::class
        ]
)
internal class RealmCryptoStoreModule
