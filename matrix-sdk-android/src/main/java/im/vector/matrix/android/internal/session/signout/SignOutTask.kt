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

package im.vector.matrix.android.internal.session.signout

import android.content.Context
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.crypto.CryptoModule
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.di.*
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.SessionModule
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import io.realm.Realm
import io.realm.RealmConfiguration
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import javax.inject.Inject

internal interface SignOutTask : Task<SignOutTask.Params, Unit> {
    data class Params(
            val sigOutFromHomeserver: Boolean
    )
}

internal class DefaultSignOutTask @Inject constructor(
        private val context: Context,
        @SessionId private val sessionId: String,
        private val signOutAPI: SignOutAPI,
        private val sessionManager: SessionManager,
        private val sessionParamsStore: SessionParamsStore,
        @SessionDatabase private val clearSessionDataTask: ClearCacheTask,
        @CryptoDatabase private val clearCryptoDataTask: ClearCacheTask,
        @UserCacheDirectory private val userFile: File,
        private val realmKeysUtils: RealmKeysUtils,
        @SessionDatabase private val realmSessionConfiguration: RealmConfiguration,
        @CryptoDatabase private val realmCryptoConfiguration: RealmConfiguration,
        @UserMd5 private val userMd5: String,
        private val eventBus: EventBus
) : SignOutTask {

    override suspend fun execute(params: SignOutTask.Params) {
        // It should be done even after a soft logout, to be sure the deviceId is deleted on the
        if (params.sigOutFromHomeserver) {
            Timber.d("SignOut: send request...")
            try {
                executeRequest<Unit>(eventBus) {
                    apiCall = signOutAPI.signOut()
                }
            } catch (throwable: Throwable) {
                // Maybe due to https://github.com/matrix-org/synapse/issues/5755
                if (throwable is Failure.ServerError
                        && throwable.httpCode == HttpURLConnection.HTTP_UNAUTHORIZED /* 401 */
                        && throwable.error.code == MatrixError.M_UNKNOWN_TOKEN) {
                    // Also throwable.error.isSoftLogout should be true
                    // Ignore
                    Timber.w("Ignore error due to https://github.com/matrix-org/synapse/issues/5755")
                } else {
                    throw throwable
                }
            }
        }

        Timber.d("SignOut: release session...")
        sessionManager.releaseSession(sessionId)

        Timber.d("SignOut: cancel pending works...")
        WorkManagerUtil.cancelAllWorks(context)

        Timber.d("SignOut: delete session params...")
        sessionParamsStore.delete(sessionId)

        Timber.d("SignOut: clear session data...")
        clearSessionDataTask.execute(Unit)

        Timber.d("SignOut: clear crypto data...")
        clearCryptoDataTask.execute(Unit)

        Timber.d("SignOut: clear file system")
        userFile.deleteRecursively()

        Timber.d("SignOut: clear the database keys")
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
