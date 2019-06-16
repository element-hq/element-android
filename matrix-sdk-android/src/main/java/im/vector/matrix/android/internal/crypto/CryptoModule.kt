/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto

import android.content.Context
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.keysbackup.api.RoomKeysApi
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStoreMigration
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStoreModule
import im.vector.matrix.android.internal.crypto.store.db.hash
import im.vector.matrix.android.internal.session.SessionScope
import io.realm.RealmConfiguration
import retrofit2.Retrofit
import java.io.File
import javax.inject.Named

@Module
internal class CryptoModule {

    // Realm configuration, named to avoid clash with main cache realm configuration
    @Provides
    @SessionScope
    @Named("CryptonRealmConfiguration")
    fun providesRealmConfiguration(context: Context, credentials: Credentials): RealmConfiguration {
        return RealmConfiguration.Builder()
                .directory(File(context.filesDir, credentials.userId.hash()))
                .name("crypto_store.realm")
                .modules(RealmCryptoStoreModule())
                .schemaVersion(RealmCryptoStoreMigration.CRYPTO_STORE_SCHEMA_VERSION)
                .migration(RealmCryptoStoreMigration)
                .build()
    }

    @Provides
    @SessionScope
    fun providesCryptoStore(@Named("CryptonRealmConfiguration")
                            realmConfiguration: RealmConfiguration, credentials: Credentials): IMXCryptoStore {
        return RealmCryptoStore(false /* TODO*/,
                realmConfiguration,
                credentials)
    }

    @Provides
    @SessionScope
    fun providesCryptoAPI(retrofit: Retrofit): CryptoApi {
        return retrofit.create(CryptoApi::class.java)
    }

    @Provides
    @SessionScope
    fun providesRoomKeysAPI(retrofit: Retrofit): RoomKeysApi {
        return retrofit.create(RoomKeysApi::class.java)
    }

    @Provides
    @SessionScope
    fun providesCryptoConfig(): MXCryptoConfig {
        return MXCryptoConfig()
    }


}
