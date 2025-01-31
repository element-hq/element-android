/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.contentscanner

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.UserMd5
import org.matrix.android.sdk.internal.session.SessionModule
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.session.contentscanner.db.ContentScannerRealmModule
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
        @SessionScope
        fun providesContentScannerRealmConfiguration(
                realmKeysUtils: RealmKeysUtils,
                @SessionFilesDirectory directory: File,
                @UserMd5 userMd5: String
        ): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .directory(directory)
                    .name("matrix-sdk-content-scanning.realm")
                    .apply {
                        realmKeysUtils.configureEncryption(this, SessionModule.getKeyAlias(userMd5))
                    }
                    .allowWritesOnUiThread(true)
                    .modules(ContentScannerRealmModule())
                    .build()
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
