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
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.internal.crypto.actions.*
import im.vector.matrix.android.internal.crypto.algorithms.megolm.MXMegolmDecryptionFactory
import im.vector.matrix.android.internal.crypto.algorithms.megolm.MXMegolmEncryptionFactory
import im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmDecryptionFactory
import im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmEncryptionFactory
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.keysbackup.api.RoomKeysApi
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.*
import im.vector.matrix.android.internal.crypto.repository.WarnOnUnknownDeviceRepository
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStoreMigration
import im.vector.matrix.android.internal.crypto.store.db.RealmCryptoStoreModule
import im.vector.matrix.android.internal.crypto.store.db.hash
import im.vector.matrix.android.internal.crypto.tasks.*
import im.vector.matrix.android.internal.crypto.verification.DefaultSasVerificationService
import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.session.cache.RealmClearCacheTask
import io.realm.RealmConfiguration
import org.koin.dsl.module.module
import org.matrix.olm.OlmManager
import retrofit2.Retrofit
import java.io.File

internal class CryptoModule {

    val definition = module(override = true) {

        /* ==========================================================================================
         * Crypto Main
         * ========================================================================================== */

        // Realm configuration, named to avoid clash with main cache realm configuration
        scope(DefaultSession.SCOPE, name = "CryptoRealmConfiguration") {
            val context: Context = get()

            val credentials: Credentials = get()

            RealmConfiguration.Builder()
                    .directory(File(context.filesDir, credentials.userId.hash()))
                    .name("crypto_store.realm")
                    .modules(RealmCryptoStoreModule())
                    .schemaVersion(RealmCryptoStoreMigration.CRYPTO_STORE_SCHEMA_VERSION)
                    .migration(RealmCryptoStoreMigration)
                    .build()
        }

        // CryptoStore
        scope(DefaultSession.SCOPE) {
            RealmCryptoStore(false /* TODO*/,
                    get("CryptoRealmConfiguration"),
                    get()) as IMXCryptoStore
        }

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(CryptoApi::class.java)
        }

        // CryptoService
        scope(DefaultSession.SCOPE) {
            get<CryptoManager>() as CryptoService
        }

        //
        scope(DefaultSession.SCOPE) {
            OutgoingRoomKeyRequestManager(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            IncomingRoomKeyRequestManager(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            RoomDecryptorProvider(get(), get())
        }

        scope(DefaultSession.SCOPE) {
            // Ensure OlmManager is loaded first
            get<OlmManager>()
            MXOlmDevice(get())
        }

        // ObjectSigner
        scope(DefaultSession.SCOPE) {
            ObjectSigner(get(), get())
        }

        // OneTimeKeysUploader
        scope(DefaultSession.SCOPE) {
            OneTimeKeysUploader(get(), get(), get(), get())
        }

        // Actions
        scope(DefaultSession.SCOPE) {
            SetDeviceVerificationAction(get(), get(), get())
        }

        // Device info
        scope(DefaultSession.SCOPE) {
            MyDeviceInfoHolder(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            EnsureOlmSessionsForDevicesAction(get(), get())
        }

        scope(DefaultSession.SCOPE) {
            EnsureOlmSessionsForUsersAction(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            MegolmSessionDataImporter(get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            MessageEncrypter(get(), get())
        }


        scope(DefaultSession.SCOPE) {
            WarnOnUnknownDeviceRepository()
        }

        // Factories
        scope(DefaultSession.SCOPE) {
            MXMegolmDecryptionFactory(
                    get(), get(), get(), get(), get(), get(), get(), get(), get()
            )
        }

        scope(DefaultSession.SCOPE) {
            MXMegolmEncryptionFactory(
                    get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
            )
        }

        scope(DefaultSession.SCOPE) {
            MXOlmDecryptionFactory(
                    get(), get()
            )
        }

        scope(DefaultSession.SCOPE) {
            MXOlmEncryptionFactory(
                    get(), get(), get(), get(), get(), get()
            )
        }

        // CryptoManager
        scope(DefaultSession.SCOPE) {
            CryptoManager(
                    credentials = get(),
                    myDeviceInfoHolder = get(),
                    cryptoStore = get(),
                    olmDevice = get(),
                    cryptoConfig = get(),
                    deviceListManager = get(),
                    keysBackup = get(),
                    objectSigner = get(),
                    oneTimeKeysUploader = get(),
                    roomDecryptorProvider = get(),
                    sasVerificationService = get(),
                    incomingRoomKeyRequestManager = get(),
                    outgoingRoomKeyRequestManager = get(),
                    olmManager = get(),
                    setDeviceVerificationAction = get(),
                    megolmSessionDataImporter = get(),
                    warnOnUnknownDevicesRepository = get(),
                    megolmEncryptionFactory = get(),
                    olmEncryptionFactory = get(),
                    deleteDeviceTask = get(),
                    deleteDeviceWithUserPasswordTask = get(),
                    // Tasks
                    getDevicesTask = get(),
                    setDeviceNameTask = get(),
                    uploadKeysTask = get(),
                    loadRoomMembersTask = get(),
                    clearCryptoDataTask = get("ClearTaskCryptoCache"),
                    monarchy = get(),
                    coroutineDispatchers = get(),
                    taskExecutor = get()
            )
        }

        // Olm manager
        single {
            // load the crypto libs.
            OlmManager()
        }


        // Crypto config
        scope(DefaultSession.SCOPE) {
            MXCryptoConfig()
        }

        // Device list
        scope(DefaultSession.SCOPE) {
            DeviceListManager(get(), get(), get(), get(), get())
        }

        // Crypto tasks
        scope(DefaultSession.SCOPE) {
            DefaultClaimOneTimeKeysForUsersDevice(get()) as ClaimOneTimeKeysForUsersDeviceTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDeleteDeviceTask(get()) as DeleteDeviceTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDeleteDeviceWithUserPasswordTask(get(), get()) as DeleteDeviceWithUserPasswordTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDownloadKeysForUsers(get()) as DownloadKeysForUsersTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetDevicesTask(get()) as GetDevicesTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetKeyChangesTask(get()) as GetKeyChangesTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultSendToDeviceTask(get()) as SendToDeviceTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultSetDeviceNameTask(get()) as SetDeviceNameTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultUploadKeysTask(get()) as UploadKeysTask
        }

        scope(DefaultSession.SCOPE, name = "ClearTaskCryptoCache") {
            RealmClearCacheTask(get("CryptoRealmConfiguration")) as ClearCacheTask
        }

        /* ==========================================================================================
         * Keys backup
         * ========================================================================================== */

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(RoomKeysApi::class.java)
        }

        scope(DefaultSession.SCOPE) {
            KeysBackup(
                    // Credentials
                    get(),
                    // CryptoStore
                    get(),
                    get(),
                    get(),
                    get(),
                    // Task
                    get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
                    // Task executor
                    get())
        }

        // Key backup tasks
        scope(DefaultSession.SCOPE) {
            DefaultCreateKeysBackupVersionTask(get()) as CreateKeysBackupVersionTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDeleteBackupTask(get()) as DeleteBackupTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDeleteRoomSessionDataTask(get()) as DeleteRoomSessionDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDeleteRoomSessionsDataTask(get()) as DeleteRoomSessionsDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultDeleteSessionsDataTask(get()) as DeleteSessionsDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetKeysBackupLastVersionTask(get()) as GetKeysBackupLastVersionTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetKeysBackupVersionTask(get()) as GetKeysBackupVersionTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetRoomSessionDataTask(get()) as GetRoomSessionDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetRoomSessionsDataTask(get()) as GetRoomSessionsDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultGetSessionsDataTask(get()) as GetSessionsDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultStoreRoomSessionDataTask(get()) as StoreRoomSessionDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultStoreRoomSessionsDataTask(get()) as StoreRoomSessionsDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultStoreSessionsDataTask(get()) as StoreSessionsDataTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultUpdateKeysBackupVersionTask(get()) as UpdateKeysBackupVersionTask
        }

        /* ==========================================================================================
         * SAS Verification
         * ========================================================================================== */

        scope(DefaultSession.SCOPE) {
            DefaultSasVerificationService(get(), get(), get(), get(), get(), get(), get(), get())
        }

    }
}
