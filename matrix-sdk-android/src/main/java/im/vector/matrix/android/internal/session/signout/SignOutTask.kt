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
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.di.CryptoDatabase
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.UserCacheDirectory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal interface SignOutTask : Task<Unit, Unit>

internal class DefaultSignOutTask @Inject constructor(private val context: Context,
                                                      private val credentials: Credentials,
                                                      private val signOutAPI: SignOutAPI,
                                                      private val sessionManager: SessionManager,
                                                      private val sessionParamsStore: SessionParamsStore,
                                                      @SessionDatabase private val clearSessionDataTask: ClearCacheTask,
                                                      @CryptoDatabase private val clearCryptoDataTask: ClearCacheTask,
                                                      @UserCacheDirectory private val userFile: File) : SignOutTask {

    override suspend fun execute(params: Unit) {
        Timber.d("SignOut: send request...")
        executeRequest<Unit> {
            apiCall = signOutAPI.signOut()
        }

        Timber.d("SignOut: release session...")
        sessionManager.releaseSession(credentials.userId)

        Timber.d("SignOut: cancel pending works...")
        WorkManagerUtil.cancelAllWorks(context)

        Timber.d("SignOut: delete session params...")
        sessionParamsStore.delete(credentials.userId)

        Timber.d("SignOut: clear session data...")
        clearSessionDataTask.execute(Unit)

        Timber.d("SignOut: clear crypto data...")
        clearCryptoDataTask.execute(Unit)

        Timber.d("SignOut: clear file system")
        userFile.deleteRecursively()
    }
}