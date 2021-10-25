/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning

import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.sdk.internal.session.contentscanning.data.ContentScanningStore
import im.vector.matrix.android.sdk.internal.session.contentscanning.db.ContentScannerRealmModule
import im.vector.matrix.android.sdk.internal.session.contentscanning.db.RealmContentScannerStore
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.DefaultDownloadEncryptedTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.DefaultGetServerPublicKeyTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.DefaultScanEncryptedTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.DefaultScanMediaTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.DownloadEncryptedTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.GetServerPublicKeyTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.ScanEncryptedTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.ScanMediaTask
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.session.contentscanning.ContentScannerService
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.UserMd5
import org.matrix.android.sdk.internal.session.SessionModule
import org.matrix.android.sdk.internal.session.SessionScope
import java.io.File

@Module
internal abstract class ContentScannerModule {
    @Module
    companion object {

        @JvmStatic
        @Provides
        @ContentScannerDatabase
        @SessionScope
        fun providesContentScannerRealmConfiguration(realmKeysUtils: RealmKeysUtils,
                                                     @SessionFilesDirectory directory: File,
                                                     @UserMd5 userMd5: String): RealmConfiguration {
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
    abstract fun bindContentScannerService(service: DefaultContentScannerService): ContentScannerService

    @Binds
    abstract fun bindContentScannerStore(store: RealmContentScannerStore): ContentScanningStore

    @Binds
    abstract fun bindDownloadEncryptedTask(task: DefaultDownloadEncryptedTask): DownloadEncryptedTask

    @Binds
    abstract fun bindGetServerPublicKeyTask(task: DefaultGetServerPublicKeyTask): GetServerPublicKeyTask

    @Binds
    abstract fun bindScanMediaTask(task: DefaultScanMediaTask): ScanMediaTask

    @Binds
    abstract fun bindScanEncryptedTask(task: DefaultScanEncryptedTask): ScanEncryptedTask
}
