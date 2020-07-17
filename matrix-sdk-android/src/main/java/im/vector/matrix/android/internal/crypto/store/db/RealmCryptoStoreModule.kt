/*
 * Copyright 2019 New Vector Ltd
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

import im.vector.matrix.android.internal.crypto.store.db.model.CrossSigningInfoEntity
import im.vector.matrix.android.internal.crypto.store.db.model.CryptoMetadataEntity
import im.vector.matrix.android.internal.crypto.store.db.model.CryptoRoomEntity
import im.vector.matrix.android.internal.crypto.store.db.model.DeviceInfoEntity
import im.vector.matrix.android.internal.crypto.store.db.model.GossipingEventEntity
import im.vector.matrix.android.internal.crypto.store.db.model.IncomingGossipingRequestEntity
import im.vector.matrix.android.internal.crypto.store.db.model.KeyInfoEntity
import im.vector.matrix.android.internal.crypto.store.db.model.KeysBackupDataEntity
import im.vector.matrix.android.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import im.vector.matrix.android.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import im.vector.matrix.android.internal.crypto.store.db.model.OlmSessionEntity
import im.vector.matrix.android.internal.crypto.store.db.model.OutgoingGossipingRequestEntity
import im.vector.matrix.android.internal.crypto.store.db.model.SharedSessionEntity
import im.vector.matrix.android.internal.crypto.store.db.model.TrustLevelEntity
import im.vector.matrix.android.internal.crypto.store.db.model.UserEntity
import im.vector.matrix.android.internal.crypto.store.db.model.WithHeldSessionEntity
import io.realm.annotations.RealmModule

/**
 * Realm module for Crypto store classes
 */
@RealmModule(library = true,
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
            GossipingEventEntity::class,
            IncomingGossipingRequestEntity::class,
            OutgoingGossipingRequestEntity::class,
            MyDeviceLastSeenInfoEntity::class,
            WithHeldSessionEntity::class,
            SharedSessionEntity::class
        ])
internal class RealmCryptoStoreModule
