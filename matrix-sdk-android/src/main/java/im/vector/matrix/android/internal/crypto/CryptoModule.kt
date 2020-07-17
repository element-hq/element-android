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

import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.crosssigning.ComputeTrustTask
import im.vector.matrix.android.internal.crypto.crosssigning.DefaultComputeTrustTask
import im.vector.matrix.android.internal.crypto.crosssigning.DefaultCrossSigningService
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
import im.vector.matrix.android.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultClaimOneTimeKeysForUsersDevice
import im.vector.matrix.android.internal.crypto.tasks.DefaultDeleteDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultDeleteDeviceWithUserPasswordTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultDownloadKeysForUsers
import im.vector.matrix.android.internal.crypto.tasks.DefaultEncryptEventTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultGetDeviceInfoTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultGetDevicesTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultInitializeCrossSigningTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultSendEventTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultSendToDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultSendVerificationMessageTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultSetDeviceNameTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultUploadKeysTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultUploadSignaturesTask
import im.vector.matrix.android.internal.crypto.tasks.DefaultUploadSigningKeysTask
import im.vector.matrix.android.internal.crypto.tasks.DeleteDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DeleteDeviceWithUserPasswordTask
import im.vector.matrix.android.internal.crypto.tasks.DownloadKeysForUsersTask
import im.vector.matrix.android.internal.crypto.tasks.EncryptEventTask
import im.vector.matrix.android.internal.crypto.tasks.GetDeviceInfoTask
import im.vector.matrix.android.internal.crypto.tasks.GetDevicesTask
import im.vector.matrix.android.internal.crypto.tasks.InitializeCrossSigningTask
import im.vector.matrix.android.internal.crypto.tasks.SendEventTask
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.SendVerificationMessageTask
import im.vector.matrix.android.internal.crypto.tasks.SetDeviceNameTask
import im.vector.matrix.android.internal.crypto.tasks.UploadKeysTask
import im.vector.matrix.android.internal.crypto.tasks.UploadSignaturesTask
import im.vector.matrix.android.internal.crypto.tasks.UploadSigningKeysTask
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.di.CryptoDatabase
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.session.cache.RealmClearCacheTask
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import retrofit2.Retrofit
import java.io.File

@Module
internal abstract class CryptoModule {

    @Module
    companion object {
        internal fun getKeyAlias(userMd5: String) = "crypto_module_$userMd5"

        @JvmStatic
        @Provides
        @CryptoDatabase
        @SessionScope
        fun providesRealmConfiguration(@SessionFilesDirectory directory: File,
                                       @UserMd5 userMd5: String,
                                       realmCryptoStoreMigration: RealmCryptoStoreMigration,
                                       realmKeysUtils: RealmKeysUtils): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .directory(directory)
                    .apply {
                        realmKeysUtils.configureEncryption(this, getKeyAlias(userMd5))
                    }
                    .name("crypto_store.realm")
                    .modules(RealmCryptoStoreModule())
                    .schemaVersion(RealmCryptoStoreMigration.CRYPTO_STORE_SCHEMA_VERSION)
                    .migration(realmCryptoStoreMigration)
                    .build()
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesCryptoCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob())
        }

        @JvmStatic
        @Provides
        @CryptoDatabase
        fun providesClearCacheTask(@CryptoDatabase
                                   realmConfiguration: RealmConfiguration): ClearCacheTask {
            return RealmClearCacheTask(realmConfiguration)
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
    }

    @Binds
    abstract fun bindCryptoService(service: DefaultCryptoService): CryptoService

    @Binds
    abstract fun bindDeleteDeviceTask(task: DefaultDeleteDeviceTask): DeleteDeviceTask

    @Binds
    abstract fun bindGetDevicesTask(task: DefaultGetDevicesTask): GetDevicesTask

    @Binds
    abstract fun bindGetDeviceInfoTask(task: DefaultGetDeviceInfoTask): GetDeviceInfoTask

    @Binds
    abstract fun bindSetDeviceNameTask(task: DefaultSetDeviceNameTask): SetDeviceNameTask

    @Binds
    abstract fun bindUploadKeysTask(task: DefaultUploadKeysTask): UploadKeysTask

    @Binds
    abstract fun bindUploadSigningKeysTask(task: DefaultUploadSigningKeysTask): UploadSigningKeysTask

    @Binds
    abstract fun bindUploadSignaturesTask(task: DefaultUploadSignaturesTask): UploadSignaturesTask

    @Binds
    abstract fun bindDownloadKeysForUsersTask(task: DefaultDownloadKeysForUsers): DownloadKeysForUsersTask

    @Binds
    abstract fun bindCreateKeysBackupVersionTask(task: DefaultCreateKeysBackupVersionTask): CreateKeysBackupVersionTask

    @Binds
    abstract fun bindDeleteBackupTask(task: DefaultDeleteBackupTask): DeleteBackupTask

    @Binds
    abstract fun bindDeleteRoomSessionDataTask(task: DefaultDeleteRoomSessionDataTask): DeleteRoomSessionDataTask

    @Binds
    abstract fun bindDeleteRoomSessionsDataTask(task: DefaultDeleteRoomSessionsDataTask): DeleteRoomSessionsDataTask

    @Binds
    abstract fun bindDeleteSessionsDataTask(task: DefaultDeleteSessionsDataTask): DeleteSessionsDataTask

    @Binds
    abstract fun bindGetKeysBackupLastVersionTask(task: DefaultGetKeysBackupLastVersionTask): GetKeysBackupLastVersionTask

    @Binds
    abstract fun bindGetKeysBackupVersionTask(task: DefaultGetKeysBackupVersionTask): GetKeysBackupVersionTask

    @Binds
    abstract fun bindGetRoomSessionDataTask(task: DefaultGetRoomSessionDataTask): GetRoomSessionDataTask

    @Binds
    abstract fun bindGetRoomSessionsDataTask(task: DefaultGetRoomSessionsDataTask): GetRoomSessionsDataTask

    @Binds
    abstract fun bindGetSessionsDataTask(task: DefaultGetSessionsDataTask): GetSessionsDataTask

    @Binds
    abstract fun bindStoreRoomSessionDataTask(task: DefaultStoreRoomSessionDataTask): StoreRoomSessionDataTask

    @Binds
    abstract fun bindStoreRoomSessionsDataTask(task: DefaultStoreRoomSessionsDataTask): StoreRoomSessionsDataTask

    @Binds
    abstract fun bindStoreSessionsDataTask(task: DefaultStoreSessionsDataTask): StoreSessionsDataTask

    @Binds
    abstract fun bindUpdateKeysBackupVersionTask(task: DefaultUpdateKeysBackupVersionTask): UpdateKeysBackupVersionTask

    @Binds
    abstract fun bindSendToDeviceTask(task: DefaultSendToDeviceTask): SendToDeviceTask

    @Binds
    abstract fun bindEncryptEventTask(task: DefaultEncryptEventTask): EncryptEventTask

    @Binds
    abstract fun bindSendVerificationMessageTask(task: DefaultSendVerificationMessageTask): SendVerificationMessageTask

    @Binds
    abstract fun bindClaimOneTimeKeysForUsersDeviceTask(task: DefaultClaimOneTimeKeysForUsersDevice): ClaimOneTimeKeysForUsersDeviceTask

    @Binds
    abstract fun bindDeleteDeviceWithUserPasswordTask(task: DefaultDeleteDeviceWithUserPasswordTask): DeleteDeviceWithUserPasswordTask

    @Binds
    abstract fun bindCrossSigningService(service: DefaultCrossSigningService): CrossSigningService

    @Binds
    abstract fun bindCryptoStore(store: RealmCryptoStore): IMXCryptoStore

    @Binds
    abstract fun bindComputeShieldTrustTask(task: DefaultComputeTrustTask): ComputeTrustTask

    @Binds
    abstract fun bindInitializeCrossSigningTask(task: DefaultInitializeCrossSigningTask): InitializeCrossSigningTask

    @Binds
    abstract fun bindSendEventTask(task: DefaultSendEventTask): SendEventTask
}
