/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.database

import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.GossipingEventEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.IncomingGossipingRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingGossipingRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import timber.log.Timber

object RealmDebugTools {
    /**
     * Log info about the crypto DB
     */
    fun dumpCryptoDb(realmConfiguration: RealmConfiguration) {
        Realm.getInstance(realmConfiguration).use {
            Timber.d("Realm located at : ${realmConfiguration.realmDirectory}/${realmConfiguration.realmFileName}")

            val key = realmConfiguration.encryptionKey.joinToString("") { byte -> "%02x".format(byte) }
            Timber.d("Realm encryption key : $key")

            // Check if we have data
            Timber.e("Realm is empty: ${it.isEmpty}")

            Timber.d("Realm has CryptoMetadataEntity: ${it.where<CryptoMetadataEntity>().count()}")
            Timber.d("Realm has CryptoRoomEntity: ${it.where<CryptoRoomEntity>().count()}")
            Timber.d("Realm has DeviceInfoEntity: ${it.where<DeviceInfoEntity>().count()}")
            Timber.d("Realm has KeysBackupDataEntity: ${it.where<KeysBackupDataEntity>().count()}")
            Timber.d("Realm has OlmInboundGroupSessionEntity: ${it.where<OlmInboundGroupSessionEntity>().count()}")
            Timber.d("Realm has OlmSessionEntity: ${it.where<OlmSessionEntity>().count()}")
            Timber.d("Realm has UserEntity: ${it.where<UserEntity>().count()}")
            Timber.d("Realm has KeyInfoEntity: ${it.where<KeyInfoEntity>().count()}")
            Timber.d("Realm has CrossSigningInfoEntity: ${it.where<CrossSigningInfoEntity>().count()}")
            Timber.d("Realm has TrustLevelEntity: ${it.where<TrustLevelEntity>().count()}")
            Timber.d("Realm has GossipingEventEntity: ${it.where<GossipingEventEntity>().count()}")
            Timber.d("Realm has IncomingGossipingRequestEntity: ${it.where<IncomingGossipingRequestEntity>().count()}")
            Timber.d("Realm has OutgoingGossipingRequestEntity: ${it.where<OutgoingGossipingRequestEntity>().count()}")
            Timber.d("Realm has MyDeviceLastSeenInfoEntity: ${it.where<MyDeviceLastSeenInfoEntity>().count()}")
        }
    }
}
