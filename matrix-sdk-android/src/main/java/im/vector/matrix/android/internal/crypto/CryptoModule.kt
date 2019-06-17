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
import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.keysbackup.api.RoomKeysApi
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.CreateKeysBackupVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultCreateKeysBackupVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultDeleteBackupTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultDeleteRoomSessionDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultDeleteRoomSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultDeleteSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultGetKeysBackupLastVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultGetKeysBackupVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultGetRoomSessionDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultGetRoomSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultGetSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultStoreRoomSessionDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultStoreRoomSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultStoreSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DefaultUpdateKeysBackupVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DeleteBackupTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DeleteRoomSessionDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DeleteRoomSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.DeleteSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.GetKeysBackupLastVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.GetKeysBackupVersionTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.GetRoomSessionDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.GetRoomSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.GetSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.StoreRoomSessionDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.StoreRoomSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.StoreSessionsDataTask
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.UpdateKeysBackupVersionTask
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStoreMigration
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStoreModule
import im.vector.matrix.android.internal.crypto.store.db.hash
import im.vector.matrix.android.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultClaimOneTimeKeysForUsersDevice
import im.vector.matrix.android.internal.crypto.tasks.DefaultDeleteDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultDownloadKeysForUsers
import im.vector.matrix.android.internal.crypto.tasks.DefaultGetDevicesTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultSendToDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultSetDeviceNameTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultUploadKeysTask
import im.vector.matrix.android.internal.crypto.tasks.DeleteDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DownloadKeysForUsersTask
import im.vector.matrix.android.internal.crypto.tasks.GetDevicesTask
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.SetDeviceNameTask
import im.vector.matrix.android.internal.crypto.tasks.UploadKeysTask
import im.vector.matrix.android.internal.di.CryptoDatabase
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.session.cache.RealmClearCacheTask
import io.realm.RealmConfiguration
import retrofit2.Retrofit
import java.io.File

@Module
internal abstract class CryptoModule {

    @Module
    companion object {

        // Realm configuration, named to avoid clash with main cache realm configuration
        @JvmStatic
        @Provides
        @SessionScope
        @CryptoDatabase
        fun providesRealmConfiguration(context: Context, credentials: Credentials): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .directory(File(context.filesDir, credentials.userId.hash()))
                    .name("crypto_store.realm")
                    .modules(RealmCryptoStoreModule())
                    .schemaVersion(RealmCryptoStoreMigration.CRYPTO_STORE_SCHEMA_VERSION)
                    .migration(RealmCryptoStoreMigration)
                    .build()
        }

        @JvmStatic
        @Provides
        @SessionScope
        @CryptoDatabase
        fun providesClearCacheTask(@CryptoDatabase realmConfiguration: RealmConfiguration): ClearCacheTask {
            return RealmClearCacheTask(realmConfiguration)
        }


        @JvmStatic
        @Provides
        @SessionScope
        fun providesCryptoStore(@CryptoDatabase realmConfiguration: RealmConfiguration, credentials: Credentials): IMXCryptoStore {
            return RealmCryptoStore(false /* TODO*/,
                                    realmConfiguration,
                                    credentials)
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesCryptoAPI(retrofit: Retrofit): CryptoApi {
            return retrofit.create(CryptoApi::class.java)
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesRoomKeysAPI(retrofit: Retrofit): RoomKeysApi {
            return retrofit.create(RoomKeysApi::class.java)
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesCryptoConfig(): MXCryptoConfig {
            return MXCryptoConfig()
        }

    }

    @Binds
    @SessionScope
    abstract fun bindCryptoService(cryptoManager: CryptoManager): CryptoService

    @Binds
    @SessionScope
    abstract fun bindDeleteDeviceTask(deleteDeviceTask: DefaultDeleteDeviceTask): DeleteDeviceTask

    @Binds
    @SessionScope
    abstract fun bindGetDevicesTask(getDevicesTask: DefaultGetDevicesTask): GetDevicesTask

    @Binds
    @SessionScope
    abstract fun bindSetDeviceNameTask(getDevicesTask: DefaultSetDeviceNameTask): SetDeviceNameTask

    @Binds
    @SessionScope
    abstract fun bindUploadKeysTask(getDevicesTask: DefaultUploadKeysTask): UploadKeysTask

    @Binds
    @SessionScope
    abstract fun bindDownloadKeysForUsersTask(downloadKeysForUsers: DefaultDownloadKeysForUsers): DownloadKeysForUsersTask

    @Binds
    @SessionScope
    abstract fun bindCreateKeysBackupVersionTask(createKeysBackupVersionTask: DefaultCreateKeysBackupVersionTask): CreateKeysBackupVersionTask

    @Binds
    @SessionScope
    abstract fun bindDeleteBackupTask(deleteBackupTask: DefaultDeleteBackupTask): DeleteBackupTask

    @Binds
    @SessionScope
    abstract fun bindDeleteRoomSessionDataTask(deleteRoomSessionDataTask: DefaultDeleteRoomSessionDataTask): DeleteRoomSessionDataTask

    @Binds
    @SessionScope
    abstract fun bindDeleteRoomSessionsDataTask(deleteRoomSessionDataTask: DefaultDeleteRoomSessionsDataTask): DeleteRoomSessionsDataTask

    @Binds
    @SessionScope
    abstract fun bindDeleteSessionsDataTask(deleteRoomSessionDataTask: DefaultDeleteSessionsDataTask): DeleteSessionsDataTask

    @Binds
    @SessionScope
    abstract fun bindGetKeysBackupLastVersionTask(getKeysBackupLastVersionTask: DefaultGetKeysBackupLastVersionTask): GetKeysBackupLastVersionTask

    @Binds
    @SessionScope
    abstract fun bindGetKeysBackupVersionTask(getKeysBackupVersionTask: DefaultGetKeysBackupVersionTask) : GetKeysBackupVersionTask

    @Binds
    @SessionScope
    abstract fun bindGetRoomSessionDataTask(getRoomSessionDataTask: DefaultGetRoomSessionDataTask) : GetRoomSessionDataTask

    @Binds
    @SessionScope
    abstract fun bindGetRoomSessionsDataTask(getRoomSessionDataTask: DefaultGetRoomSessionsDataTask) : GetRoomSessionsDataTask

    @Binds
    @SessionScope
    abstract fun bindGetSessionsDataTask(getRoomSessionDataTask: DefaultGetSessionsDataTask) : GetSessionsDataTask

    @Binds
    @SessionScope
    abstract fun bindStoreRoomSessionDataTask(storeRoomSessionDataTask: DefaultStoreRoomSessionDataTask) : StoreRoomSessionDataTask

    @Binds
    @SessionScope
    abstract fun bindStoreRoomSessionsDataTask(storeRoomSessionDataTask: DefaultStoreRoomSessionsDataTask) : StoreRoomSessionsDataTask

    @Binds
    @SessionScope
    abstract fun bindStoreSessionsDataTask(storeRoomSessionDataTask: DefaultStoreSessionsDataTask) : StoreSessionsDataTask

    @Binds
    @SessionScope
    abstract fun bindUpdateKeysBackupVersionTask(updateKeysBackupVersionTask: DefaultUpdateKeysBackupVersionTask) : UpdateKeysBackupVersionTask

    @Binds
    @SessionScope
    abstract fun bindSendToDeviceTask(sendToDeviceTask: DefaultSendToDeviceTask) : SendToDeviceTask

    @Binds
    @SessionScope
    abstract fun bindClaimOneTimeKeysForUsersDeviceTask(claimOneTimeKeysForUsersDevice: DefaultClaimOneTimeKeysForUsersDevice) : ClaimOneTimeKeysForUsersDeviceTask




}
