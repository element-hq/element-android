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

package im.vector.matrix.android.internal.session.cleanup

import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.crypto.CryptoModule
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.di.CryptoDatabase
import im.vector.matrix.android.internal.di.SessionDownloadsDirectory
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.SessionModule
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
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
        Timber.d("Cleanup: release session...")
        sessionManager.releaseSession(sessionId)

        Timber.d("Cleanup: cancel pending works...")
        workManagerProvider.cancelAllWorks()

        Timber.d("Cleanup: delete session params...")
        sessionParamsStore.delete(sessionId)

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
