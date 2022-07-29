/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.contentscanner

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.di.MatrixCoroutineScope
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.UserMd5
import org.matrix.android.sdk.internal.session.SessionModule
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.session.contentscanner.db.CONTENT_SCANNER_REALM_SCHEMA
import org.matrix.android.sdk.internal.session.contentscanner.db.RealmContentScannerStore
import org.matrix.android.sdk.internal.session.contentscanner.tasks.DefaultDownloadEncryptedTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.DefaultGetServerPublicKeyTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.DefaultScanEncryptedTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.DefaultScanMediaTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.DownloadEncryptedTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.GetServerPublicKeyTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.ScanEncryptedTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.ScanMediaTask
import java.io.File

@Module
internal abstract class ContentScannerModule {
    @Module
    companion object {

        @JvmStatic
        @Provides
        @ContentScannerDatabase
        fun providesRealmConfiguration(
                realmKeysUtils: RealmKeysUtils,
                @SessionFilesDirectory directory: File,
                @UserMd5 userMd5: String
        ): RealmConfiguration {
            return RealmConfiguration.Builder(CONTENT_SCANNER_REALM_SCHEMA)
                    .directory(directory.path)
                    .apply {
                        realmKeysUtils.configureEncryption(this, SessionModule.getKeyAlias(userMd5))
                    }
                    .name("matrix-sdk-content-scanning.realm")
                    .build()
        }

        @JvmStatic
        @Provides
        @ContentScannerDatabase
        @SessionScope
        fun providesRealmInstance(
                @ContentScannerDatabase realmConfiguration: RealmConfiguration,
                @MatrixCoroutineScope matrixCoroutineScope: CoroutineScope,
                matrixCoroutineDispatchers: MatrixCoroutineDispatchers
        ): RealmInstance {
            return RealmInstance(
                    coroutineScope = matrixCoroutineScope,
                    realmConfiguration = realmConfiguration,
                    coroutineDispatcher = matrixCoroutineDispatchers.io
            )
        }
    }

    @Binds
    abstract fun bindContentScannerService(service: DisabledContentScannerService): ContentScannerService

    @Binds
    abstract fun bindContentScannerStore(store: RealmContentScannerStore): ContentScannerStore

    @Binds
    abstract fun bindDownloadEncryptedTask(task: DefaultDownloadEncryptedTask): DownloadEncryptedTask

    @Binds
    abstract fun bindGetServerPublicKeyTask(task: DefaultGetServerPublicKeyTask): GetServerPublicKeyTask

    @Binds
    abstract fun bindScanMediaTask(task: DefaultScanMediaTask): ScanMediaTask

    @Binds
    abstract fun bindScanEncryptedTask(task: DefaultScanEncryptedTask): ScanEncryptedTask
}
