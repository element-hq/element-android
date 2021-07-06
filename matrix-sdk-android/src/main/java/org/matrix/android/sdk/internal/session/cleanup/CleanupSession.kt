/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.cleanup

import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.crypto.CryptoModule
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.SessionDownloadsDirectory
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UserMd5
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.SessionModule
import org.matrix.android.sdk.internal.session.cache.ClearCacheTask
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class CleanupSession @Inject constructor(
        private val workManagerProvider: WorkManagerProvider,
        @SessionId private val sessionId: String,
        private val sessionManager: SessionManager,
        private val sessionParamsStore: SessionParamsStore,
        @SessionDatabase private val clearSessionDataTask: ClearCacheTask,
        @CryptoDatabase private val clearCryptoDataTask: ClearCacheTask,
        @SessionFilesDirectory private val sessionFiles: File,
        @SessionDownloadsDirectory private val sessionCache: File,
        private val realmKeysUtils: RealmKeysUtils,
        @SessionDatabase private val realmSessionConfiguration: RealmConfiguration,
        @CryptoDatabase private val realmCryptoConfiguration: RealmConfiguration,
        @UserMd5 private val userMd5: String
) {
    suspend fun handle() {
        Timber.d("Cleanup: delete session params...")
        sessionParamsStore.delete(sessionId)

        Timber.d("Cleanup: cancel pending works...")
        workManagerProvider.cancelAllWorks()

        Timber.d("Cleanup: clear session data...")
        clearSessionDataTask.execute(Unit)

        Timber.d("Cleanup: clear crypto data...")
        clearCryptoDataTask.execute(Unit)

        Timber.d("Cleanup: clear file system")
        sessionFiles.deleteRecursively()
        sessionCache.deleteRecursively()

        Timber.d("Cleanup: clear the database keys")
        realmKeysUtils.clear(SessionModule.getKeyAlias(userMd5))
        realmKeysUtils.clear(CryptoModule.getKeyAlias(userMd5))

        Timber.d("Cleanup: release session...")
        sessionManager.releaseSession(sessionId)

        // Sanity check
        if (BuildConfig.DEBUG) {
            Realm.getGlobalInstanceCount(realmSessionConfiguration)
                    .takeIf { it > 0 }
                    ?.let { Timber.e("All realm instance for session has not been closed ($it)") }
            Realm.getGlobalInstanceCount(realmCryptoConfiguration)
                    .takeIf { it > 0 }
                    ?.let { Timber.e("All realm instance for crypto has not been closed ($it)") }
        }
    }
}
